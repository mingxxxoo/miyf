package cn.miyf.infrastructure.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 搜索门面：统一索引写入与查询入口。
 * 业务只依赖本接口；具体实现可为 Elasticsearch 或 Noop 降级，由 {@code app.search.enabled} 决定。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface SearchClient {

    /**
     * 判断是否已连接真实 Elasticsearch。
     * Noop 实现恒为 false，可用于业务侧决定是否走 ES 召回。
     *
     * @return true 表示已启用 ES
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    boolean isEnabled();

    /**
     * 探活 Elasticsearch 集群。
     *
     * @return 可达返回 true；Noop 恒为 false
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    boolean ping();

    /**
     * 解析物理索引名（含配置前缀）。
     *
     * @param logicalName 逻辑名，如 kitchen_dish
     * @return 物理索引名
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    String resolveIndex(String logicalName);

    /**
     * 确保索引存在；不存在则按动态映射创建。
     *
     * @param logicalName 逻辑索引名
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void ensureIndex(String logicalName);

    /**
     * 确保索引存在，并可应用自定义 mapping。
     *
     * @param logicalName 逻辑索引名
     * @param mappingJson 索引定义 JSON；为空则动态映射
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void ensureIndex(String logicalName, String mappingJson);

    /**
     * 按 ID 写入或覆盖文档。
     *
     * @param logicalName 逻辑索引名
     * @param id          文档 ID
     * @param document    文档对象（Jackson 序列化）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void index(String logicalName, String id, Object document);

    /**
     * 批量写入文档。
     *
     * @param logicalName 逻辑索引名
     * @param documents   文档列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void bulkIndex(String logicalName, Collection<IndexDocument> documents);

    /**
     * 按 ID 删除文档。
     *
     * @param logicalName 逻辑索引名
     * @param id          文档 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void delete(String logicalName, String id);

    /**
     * 按 ID 批量删除文档。
     *
     * @param logicalName 逻辑索引名
     * @param ids         文档 ID 集合
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void delete(String logicalName, Collection<String> ids);

    /**
     * 按 ID 获取文档。
     *
     * @param logicalName 逻辑索引名
     * @param id          文档 ID
     * @param type        反序列化目标类型
     * @param <T>         文档类型
     * @return 文档；不存在返回 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <T> T get(String logicalName, String id, Class<T> type);

    /**
     * 执行搜索并返回带 _source 的分页结果。
     *
     * @param query 查询条件
     * @param type  命中文档类型
     * @param <T>   文档类型
     * @return 分页结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <T> SearchPage<T> search(SearchQuery query, Class<T> type);

    /**
     * 仅返回命中文档 ID。
     * 常用于先 ES 召回再回表补全，减少网络载荷。
     *
     * @param query 查询条件
     * @return 文档 ID 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<String> searchIds(SearchQuery query);

    /**
     * 按查询返回 ID 分页（含总数）；业务层据此回表，无需接触 SearchPage/Hit。
     *
     * @param query    已含 from/size 的查询
     * @param page     页码（与 AbstractCondition 一致，从 1 开始）
     * @param pageSize 页大小
     * @return ID 分页
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default SearchIdPage searchIdPage(SearchQuery query, int page, int pageSize) {
        if (query == null || !isEnabled()) {
            return SearchIdPage.empty(Math.max(page, 1), Math.max(pageSize, 1));
        }
        long total = count(query);
        List<String> ids = searchIds(query);
        return new SearchIdPage(ids, total, Math.max(page, 1), Math.max(pageSize, 1));
    }

    /**
     * 统计匹配文档数（忽略分页参数）。
     *
     * @param query 查询条件
     * @return 命中总数
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    long count(SearchQuery query);

    /**
     * 仅按 term 过滤的便捷查询（无关键词）。
     *
     * @param logicalName 逻辑索引
     * @param filters     字段过滤；值可为单值或集合
     * @param from        偏移
     * @param size        页大小
     * @param type        文档类型
     * @param <T>         文档类型
     * @return 分页结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default <T> SearchPage<T> searchByFilters(String logicalName,
                                              Map<String, Object> filters,
                                              int from,
                                              int size,
                                              Class<T> type) {
        SearchQuery query = new SearchQuery().setIndex(logicalName).setFrom(from).setSize(size);
        if (filters != null) {
            filters.forEach(query::filter);
        }
        return search(query, type);
    }
}
