package cn.miyf.kitchen.search;

import cn.miyf.infrastructure.search.SearchClient;
import cn.miyf.infrastructure.search.SearchIdPage;
import cn.miyf.infrastructure.search.SearchQueries;
import cn.miyf.infrastructure.search.SearchQuery;
import cn.miyf.kitchen.bean.document.DishSearchDocument;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.service.SystemConfigReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜品搜索：索引维护 + 基于 {@link DishPageQo}/{@link cn.miyf.common.query.AbstractCondition} 的召回。
 * 仅 ON_SALE 入索引；业务不直接接触 Elasticsearch Java API。
 * <p>
 * 召回开关：ES 客户端启用、系统配置 {@code search.enabled=true}、集群 ping 成功且索引内已有文档。
 * 未 reindex 或集群不可达时回落 SQL，避免空列表。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DishSearchIndexService {

    /** 与系统配置键一致。 */
    public static final String CONFIG_SEARCH_ENABLED = "search.enabled";

    /** 索引文档探针缓存 TTL，避免每次列表都 count。 */
    private static final long RECALL_PROBE_TTL_MS = 60_000L;

    private final SearchClient searchClient;
    private final SystemConfigReader systemConfigReader;

    private volatile boolean indexReady;
    private volatile long recallProbeAtMillis;
    private volatile boolean recallProbeOk;

    /**
     * 是否可走 ES 召回：客户端启用、配置打开、集群可达且索引已有文档。
     *
     * @return true 可 ES 召回
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean isRecallEnabled() {
        if (!searchClient.isEnabled()
                || !systemConfigReader.getBoolean(CONFIG_SEARCH_ENABLED, false)) {
            return false;
        }
        return probeRecallReady();
    }

    /**
     * 探活 + 索引文档数；结果短时缓存。无文档时回落 SQL。
     */
    private boolean probeRecallReady() {
        long now = System.currentTimeMillis();
        if (now - recallProbeAtMillis < RECALL_PROBE_TTL_MS) {
            return recallProbeOk;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - recallProbeAtMillis < RECALL_PROBE_TTL_MS) {
                return recallProbeOk;
            }
            boolean ok = false;
            try {
                if (!searchClient.ping()) {
                    log.debug("ES recall skipped: ping failed");
                } else {
                    ensureIndexReady();
                    long docs = searchClient.count(new SearchQuery()
                            .setIndex(DishSearchDocument.INDEX)
                            .setFrom(0)
                            .setSize(0));
                    ok = docs > 0;
                    if (!ok) {
                        log.debug("ES recall skipped: index empty, fallback SQL");
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("ES recall probe failed: {}", ex.getMessage());
                ok = false;
            }
            recallProbeOk = ok;
            recallProbeAtMillis = System.currentTimeMillis();
            return ok;
        }
    }

    /**
     * 按菜品状态同步索引：上架写入，非上架删除（仅当 ES 客户端可用）。
     *
     * @param dish 已持久化菜品
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void sync(Dish dish) {
        if (dish == null || dish.getId() == null || !searchClient.isEnabled()) {
            return;
        }
        if ("ON_SALE".equals(dish.getStatus())) {
            indexOnSale(dish);
        } else {
            remove(dish.getId());
        }
    }

    /**
     * 写入或覆盖上架菜品文档。
     *
     * @param dish 菜品
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void indexOnSale(Dish dish) {
        if (dish == null || dish.getId() == null || !"ON_SALE".equals(dish.getStatus())) {
            return;
        }
        if (!searchClient.isEnabled()) {
            return;
        }
        ensureIndexReady();
        try {
            searchClient.index(DishSearchDocument.INDEX, String.valueOf(dish.getId()), DishSearchDocument.from(dish));
            // 写入后允许下次探针重新评估文档数
            recallProbeAtMillis = 0L;
        } catch (RuntimeException ex) {
            log.warn("写入菜品 ES 失败 dishId={}: {}", dish.getId(), ex.getMessage());
        }
    }

    /**
     * 从索引移除菜品（下架 / 删除）。
     *
     * @param dishId 菜品 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void remove(Long dishId) {
        if (dishId == null || !searchClient.isEnabled()) {
            return;
        }
        ensureIndexReady();
        try {
            searchClient.delete(DishSearchDocument.INDEX, String.valueOf(dishId));
            // 删除后重新探针，避免索引已空仍走 ES 召回
            recallProbeAtMillis = 0L;
        } catch (RuntimeException ex) {
            log.warn("删除菜品 ES 失败 dishId={}: {}", dishId, ex.getMessage());
            recallProbeAtMillis = 0L;
        }
    }

    /**
     * 用户端分页召回：分页 / 关键字 / 排序与 {@link DishPageQo}（AbstractCondition）一致。
     *
     * @param qo 查询条件
     * @return ID 分页；未启用召回时为空页
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchIdPage searchUser(DishPageQo qo) {
        if (qo == null || !isRecallEnabled()) {
            int page = qo == null ? 1 : qo.normalizedPage();
            int size = qo == null ? 10 : qo.normalizedRows();
            return SearchIdPage.empty(page, size);
        }
        ensureIndexReady();
        return SearchQueries.index(DishSearchDocument.INDEX)
                .condition(qo)
                .keywordFields(DishSearchDocument.KEYWORD_FIELDS)
                .filter("categoryId", qo.getCategoryId())
                .filter("recommend", qo.getRecommend())
                .fieldMapping(DishSearchDocument.FIELD_MAPPING)
                .defaultOrder(DishSearchDocument.DEFAULT_ORDER)
                .preferScoreWhenKeyword(true)
                .searchIds(searchClient);
    }

    /**
     * 热门菜品 ID（评分优先）。
     *
     * @param limit 条数
     * @return 文档 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<String> searchHotIds(int limit) {
        if (!isRecallEnabled()) {
            return List.of();
        }
        ensureIndexReady();
        DishPageQo qo = new DishPageQo();
        qo.setPage(1);
        qo.setRows(Math.max(1, limit));
        return SearchQueries.index(DishSearchDocument.INDEX)
                .condition(qo)
                .fieldMapping(DishSearchDocument.FIELD_MAPPING)
                .defaultOrder(DishSearchDocument.HOT_ORDER)
                .preferScoreWhenKeyword(false)
                .searchIds(searchClient)
                .ids();
    }

    /**
     * 推荐菜品 ID。
     *
     * @param limit 条数
     * @return 文档 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<String> searchRecommendIds(int limit) {
        if (!isRecallEnabled()) {
            return List.of();
        }
        ensureIndexReady();
        DishPageQo qo = new DishPageQo();
        qo.setPage(1);
        qo.setRows(Math.max(1, limit));
        return SearchQueries.index(DishSearchDocument.INDEX)
                .condition(qo)
                .filter("recommend", true)
                .fieldMapping(DishSearchDocument.FIELD_MAPPING)
                .defaultOrder(DishSearchDocument.RECOMMEND_ORDER)
                .preferScoreWhenKeyword(false)
                .searchIds(searchClient)
                .ids();
    }

    private void ensureIndexReady() {
        if (indexReady || !searchClient.isEnabled()) {
            return;
        }
        synchronized (this) {
            if (indexReady) {
                return;
            }
            searchClient.ensureIndex(DishSearchDocument.INDEX);
            indexReady = true;
        }
    }
}
