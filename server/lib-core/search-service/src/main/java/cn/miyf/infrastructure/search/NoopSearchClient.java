package cn.miyf.infrastructure.search;

import cn.miyf.config.SearchAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * 搜索关闭时的空实现。
 * 保证业务层可稳定注入 {@link SearchClient}，在未启动 Elasticsearch 时不发起网络调用；
 * 写操作静默忽略，读操作返回空结果。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public class NoopSearchClient implements SearchClient {

    private static final Logger log = LoggerFactory.getLogger(NoopSearchClient.class);

    private final SearchAppProperties properties;

    /**
     * 构造 Noop 客户端并输出一次提示日志，便于排查「为何搜不到」。
     *
     * @param properties 搜索配置（仍用于 resolveIndex）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public NoopSearchClient(SearchAppProperties properties) {
        this.properties = properties;
        log.info("SearchClient 使用 Noop 实现（app.search.enabled=false）");
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean isEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean ping() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 仍按前缀规则解析，便于业务在关闭 ES 时也能拿到一致的物理索引名做日志/配置校验。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public String resolveIndex(String logicalName) {
        return properties.resolveIndex(logicalName);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void ensureIndex(String logicalName) {
        // 关闭搜索时不创建索引
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void ensureIndex(String logicalName, String mappingJson) {
        // 关闭搜索时不创建索引
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void index(String logicalName, String id, Object document) {
        // 关闭搜索时忽略写入
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void bulkIndex(String logicalName, Collection<IndexDocument> documents) {
        // 关闭搜索时忽略批量写入
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void delete(String logicalName, String id) {
        // 关闭搜索时忽略删除
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void delete(String logicalName, Collection<String> ids) {
        // 关闭搜索时忽略批量删除
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> T get(String logicalName, String id, Class<T> type) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> SearchPage<T> search(SearchQuery query, Class<T> type) {
        int from = query == null ? 0 : query.getFrom();
        int size = query == null ? 20 : query.getSize();
        return SearchPage.empty(from, size);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<String> searchIds(SearchQuery query) {
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public long count(SearchQuery query) {
        return 0L;
    }
}
