package cn.miyf.infrastructure.search;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.SearchAppProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于官方 {@link ElasticsearchClient} 的搜索实现。
 * 将业务侧逻辑索引名解析为带前缀的物理索引，并封装写入、删除、multi_match 查询与计数。
 * IO 异常统一包装为业务异常，避免向上抛出受检异常打断应用层事务语义。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchClient implements SearchClient {

    private final ElasticsearchClient client;
    private final SearchAppProperties properties;

    
    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 探活失败只记日志并返回 false，不向外抛异常，便于监控探测。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean ping() {
        try {
            return client.ping().value();
        } catch (Exception ex) {
            log.warn("Elasticsearch ping 失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * {@inheritDoc}
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
        ensureIndex(logicalName, null);
    }

    /**
     * {@inheritDoc}
     * 已存在则跳过；mappingJson 为空时依赖动态映射，非空则作为创建索引请求体。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void ensureIndex(String logicalName, String mappingJson) {
        String index = resolveIndex(logicalName);
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
            if (exists) {
                return;
            }
            CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder().index(index);
            if (StringUtils.hasText(mappingJson)) {
                // mappingJson 需是可被 CreateIndex 接受的索引定义片段
                builder.withJson(new StringReader(mappingJson));
            }
            client.indices().create(builder.build());
            log.info("已创建 Elasticsearch 索引: {}", index);
        } catch (IOException ex) {
            throw wrap(ex, "创建索引失败: " + index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void index(String logicalName, String id, Object document) {
        requireId(id);
        String index = resolveIndex(logicalName);
        try {
            client.index(IndexRequest.of(i -> i
                    .index(index)
                    .id(id)
                    .document(document)));
        } catch (IOException ex) {
            throw wrap(ex, "写入文档失败: " + index + "/" + id);
        }
    }

    /**
     * {@inheritDoc}
     * 跳过无效条目；任一子项失败时 ES 仍可能写成功部分文档，此处按 errors 标志整体报错。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void bulkIndex(String logicalName, Collection<IndexDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        String index = resolveIndex(logicalName);
        List<BulkOperation> ops = new ArrayList<>(documents.size());
        for (IndexDocument doc : documents) {
            if (doc == null || !StringUtils.hasText(doc.id()) || doc.document() == null) {
                continue;
            }
            ops.add(BulkOperation.of(op -> op.index(idx -> idx
                    .index(index)
                    .id(doc.id())
                    .document(doc.document()))));
        }
        if (ops.isEmpty()) {
            return;
        }
        try {
            BulkResponse response = client.bulk(BulkRequest.of(b -> b.operations(ops)));
            if (response.errors()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "批量写入 Elasticsearch 部分失败: " + index);
            }
        } catch (IOException ex) {
            throw wrap(ex, "批量写入失败: " + index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void delete(String logicalName, String id) {
        requireId(id);
        String index = resolveIndex(logicalName);
        try {
            client.delete(DeleteRequest.of(d -> d.index(index).id(id)));
        } catch (IOException ex) {
            throw wrap(ex, "删除文档失败: " + index + "/" + id);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void delete(String logicalName, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String index = resolveIndex(logicalName);
        List<BulkOperation> ops = new ArrayList<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            ops.add(BulkOperation.of(op -> op.delete(d -> d.index(index).id(id))));
        }
        if (ops.isEmpty()) {
            return;
        }
        try {
            client.bulk(BulkRequest.of(b -> b.operations(ops)));
        } catch (IOException ex) {
            throw wrap(ex, "批量删除失败: " + index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> T get(String logicalName, String id, Class<T> type) {
        requireId(id);
        Objects.requireNonNull(type, "type");
        String index = resolveIndex(logicalName);
        try {
            GetResponse<T> response = client.get(g -> g.index(index).id(id), type);
            return response.found() ? response.source() : null;
        } catch (IOException ex) {
            throw wrap(ex, "读取文档失败: " + index + "/" + id);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> SearchPage<T> search(SearchQuery query, Class<T> type) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(type, "type");
        String index = resolveIndex(query.getIndex());
        try {
            SearchResponse<T> response = client.search(buildSearchRequest(index, query), type);
            List<SearchHit<T>> hits = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                // score 在部分排序场景可能为空
                float score = hit.score() == null ? Float.NaN : hit.score().floatValue();
                hits.add(new SearchHit<>(hit.id(), score, hit.source()));
            }
            long total = response.hits().total() == null ? hits.size() : response.hits().total().value();
            return new SearchPage<>(hits, total, query.getFrom(), query.getSize());
        } catch (IOException ex) {
            throw wrap(ex, "搜索失败: " + index);
        }
    }

    /**
     * {@inheritDoc}
     * 关闭 _source 拉取，仅取文档 ID，适合「ES 召回 + 数据库回表」模式。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<String> searchIds(SearchQuery query) {
        Objects.requireNonNull(query, "query");
        String index = resolveIndex(query.getIndex());
        try {
            SearchResponse<Void> response = client.search(s -> {
                SearchRequest.Builder b = buildSearchRequestBuilder(index, query);
                b.source(src -> src.fetch(false));
                return b;
            }, Void.class);
            List<String> ids = new ArrayList<>();
            for (Hit<Void> hit : response.hits().hits()) {
                if (StringUtils.hasText(hit.id())) {
                    ids.add(hit.id());
                }
            }
            return ids;
        } catch (IOException ex) {
            throw wrap(ex, "搜索 ID 失败: " + index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public long count(SearchQuery query) {
        Objects.requireNonNull(query, "query");
        String index = resolveIndex(query.getIndex());
        try {
            return client.count(CountRequest.of(c -> c
                    .index(index)
                    .query(buildQuery(query)))).count();
        } catch (IOException ex) {
            throw wrap(ex, "统计失败: " + index);
        }
    }

    /**
     * 构建完整 SearchRequest。
     *
     * @param index 物理索引名
     * @param query 业务查询
     * @return 请求
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private SearchRequest buildSearchRequest(String index, SearchQuery query) {
        return buildSearchRequestBuilder(index, query).build();
    }

    /**
     * 构建 SearchRequest.Builder（含分页、查询体与排序）。
     *
     * @param index 物理索引名
     * @param query 业务查询
     * @return 可继续定制的 Builder
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private SearchRequest.Builder buildSearchRequestBuilder(String index, SearchQuery query) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(index)
                .from(query.getFrom())
                .size(query.getSize())
                .query(buildQuery(query));
        if (query.getSorts() != null) {
            for (SearchSort sort : query.getSorts()) {
                if (sort == null || !StringUtils.hasText(sort.field())) {
                    continue;
                }
                SortOrder order = sort.asc() ? SortOrder.Asc : SortOrder.Desc;
                if ("_score".equals(sort.field())) {
                    builder.sort(s -> s.score(sc -> sc.order(order)));
                } else {
                    String field = sort.field();
                    builder.sort(s -> s.field(f -> f.field(field).order(order)));
                }
            }
        }
        return builder;
    }

    /**
     * 将 SearchQuery 转为 ES Query。
     * 有关键词时用 multi_match（字段缺省为 *）；过滤值为集合时用 terms，否则用 term；
     * 无任何条件时退化为 match_all。
     *
     * @param query 业务查询
     * @return ES 查询 DSL
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private Query buildQuery(SearchQuery query) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean hasClause = false;

        if (StringUtils.hasText(query.getKeyword())) {
            List<String> fields = query.getFields() == null || query.getFields().isEmpty()
                    ? List.of("*")
                    : query.getFields();
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(query.getKeyword().trim())
                    .fields(fields)));
            hasClause = true;
        }

        Map<String, Object> filters = query.getFilters();
        if (filters != null) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                String field = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Collection<?> collection) {
                    List<FieldValue> values = new ArrayList<>();
                    for (Object item : collection) {
                        if (item != null) {
                            values.add(toFieldValue(item));
                        }
                    }
                    if (!values.isEmpty()) {
                        bool.filter(f -> f.terms(t -> t.field(field).terms(tv -> tv.value(values))));
                        hasClause = true;
                    }
                } else {
                    bool.filter(f -> f.term(t -> t.field(field).value(toFieldValue(value))));
                    hasClause = true;
                }
            }
        }

        if (!hasClause) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(bool.build()));
    }

    /**
     * 将 Java 值转为 ES FieldValue；非常见标量走 JsonData 兜底。
     *
     * @param value 原始值
     * @return FieldValue
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private static FieldValue toFieldValue(Object value) {
        if (value instanceof String s) {
            return FieldValue.of(s);
        }
        if (value instanceof Integer i) {
            return FieldValue.of(i);
        }
        if (value instanceof Long l) {
            return FieldValue.of(l);
        }
        if (value instanceof Double d) {
            return FieldValue.of(d);
        }
        if (value instanceof Float f) {
            return FieldValue.of(f.doubleValue());
        }
        if (value instanceof Boolean b) {
            return FieldValue.of(b);
        }
        return FieldValue.of(JsonData.of(value));
    }

    /**
     * 校验文档 ID 非空。
     *
     * @param id 文档 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private static void requireId(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档 ID 不能为空");
        }
    }

    /**
     * 将底层异常包装为业务异常并记录错误日志。
     *
     * @param ex      原始异常
     * @param message 业务可读说明
     * @return BusinessException
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private static BusinessException wrap(Exception ex, String message) {
        log.error("{}: {}", message, ex.getMessage());
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
}
