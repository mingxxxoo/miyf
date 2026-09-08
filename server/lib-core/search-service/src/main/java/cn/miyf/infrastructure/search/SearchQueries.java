package cn.miyf.infrastructure.search;

import cn.miyf.common.query.AbstractCondition;
import cn.miyf.common.query.SortColumn;
import cn.miyf.common.query.SortType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 基于 {@link AbstractCondition} 的搜索查询构建器。
 * 统一 page/rows/keyword/sort 语义，业务只表达意图，不拼接 ES 原生 API。
 *
 * <pre>{@code
 * SearchQuery q = SearchQueries.index("kitchen_dish")
 *     .condition(qo)
 *     .keywordFields("name^3", "subtitle^2")
 *     .filter("categoryId", categoryId)
 *     .fieldMapping(mapping)
 *     .defaultOrder("d.is_recommend DESC, d.sort_order ASC, d.create_time DESC")
 *     .preferScoreWhenKeyword(true)
 *     .build();
 * }</pre>
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class SearchQueries {

    private SearchQueries() {
    }

    /**
     * 按逻辑索引创建构建器。
     *
     * @param logicalIndex 逻辑索引名
     * @return 构建器
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static Builder index(String logicalIndex) {
        return new Builder(logicalIndex);
    }

    /**
     * 查询意图构建器。
     */
    public static final class Builder {

        private final String logicalIndex;
        private AbstractCondition condition;
        private final List<String> keywordFields = new ArrayList<>();
        private final List<Consumer<SearchQuery>> filters = new ArrayList<>();
        private SearchFieldMapping fieldMapping = SearchFieldMapping.create();
        private String defaultOrder;
        private boolean preferScoreWhenKeyword = true;
        private final List<SearchSort> extraSorts = new ArrayList<>();

        private Builder(String logicalIndex) {
            this.logicalIndex = logicalIndex;
        }

        /**
         * 绑定通用查询条件（分页 / 关键字 / 排序）。
         *
         * @param condition QO
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder condition(AbstractCondition condition) {
            this.condition = condition;
            return this;
        }

        /**
         * multi_match 字段（可含权重，如 name^3）。
         *
         * @param fields 字段
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder keywordFields(String... fields) {
            if (fields != null) {
                for (String field : fields) {
                    if (StringUtils.hasText(field)) {
                        keywordFields.add(field.trim());
                    }
                }
            }
            return this;
        }

        /**
         * multi_match 字段列表。
         *
         * @param fields 字段
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder keywordFields(List<String> fields) {
            if (fields != null) {
                for (String field : fields) {
                    if (StringUtils.hasText(field)) {
                        keywordFields.add(field.trim());
                    }
                }
            }
            return this;
        }

        /**
         * term 过滤；值为 null 时忽略。
         *
         * @param field 字段
         * @param value 值
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder filter(String field, Object value) {
            if (StringUtils.hasText(field) && value != null) {
                filters.add(q -> q.filter(field, value));
            }
            return this;
        }

        /**
         * SQL 列 → ES 字段映射。
         *
         * @param mapping 映射
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder fieldMapping(SearchFieldMapping mapping) {
            if (mapping != null) {
                this.fieldMapping = mapping;
            }
            return this;
        }

        /**
         * 无请求 sort 时的默认排序（SQL 风格：{@code col ASC, col2 DESC}）。
         *
         * @param defaultOrder 默认 ORDER BY 文本
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder defaultOrder(String defaultOrder) {
            this.defaultOrder = defaultOrder;
            return this;
        }

        /**
         * 有关键字时是否优先按相关度排序（默认 true）。
         *
         * @param prefer true 则前置 _score
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder preferScoreWhenKeyword(boolean prefer) {
            this.preferScoreWhenKeyword = prefer;
            return this;
        }

        /**
         * 追加固定排序（在条件排序之后）。
         *
         * @param sort 排序
         * @return this
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public Builder sort(SearchSort sort) {
            if (sort != null) {
                extraSorts.add(sort);
            }
            return this;
        }

        /**
         * 生成 {@link SearchQuery}。
         *
         * @return 查询
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public SearchQuery build() {
            SearchQuery query = new SearchQuery().setIndex(logicalIndex);
            for (Consumer<SearchQuery> filter : filters) {
                filter.accept(query);
            }

            int page = 1;
            int rows = 10;
            String keyword = null;
            if (condition != null) {
                page = condition.normalizedPage();
                rows = condition.normalizedRows();
                keyword = condition.getKeyword();
                query.setFrom(condition.offset()).setSize(rows);
                if (StringUtils.hasText(keyword)) {
                    query.setKeyword(keyword.trim());
                    if (!keywordFields.isEmpty()) {
                        query.setFields(keywordFields);
                    }
                }
            } else {
                query.setFrom(0).setSize(rows);
            }

            boolean hasKeyword = StringUtils.hasText(keyword);
            if (preferScoreWhenKeyword && hasKeyword) {
                query.sort(SearchSort.scoreDesc());
            }

            List<SearchSort> conditionSorts = resolveSorts(condition, defaultOrder, fieldMapping);
            for (SearchSort sort : conditionSorts) {
                query.sort(sort);
            }
            for (SearchSort sort : extraSorts) {
                query.sort(sort);
            }

            // 无任何排序时给一个稳定默认，避免结果抖动
            if (query.getSorts().isEmpty()) {
                query.sort(SearchSort.asc("_id"));
            }
            return query;
        }

        /**
         * 构建查询并执行 ID 分页召回。
         *
         * @param searchClient 搜索门面
         * @return ID 分页
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        public SearchIdPage searchIds(SearchClient searchClient) {
            SearchQuery query = build();
            int page = condition == null ? 1 : condition.normalizedPage();
            int pageSize = condition == null ? query.getSize() : condition.normalizedRows();
            if (searchClient == null || !searchClient.isEnabled()) {
                return SearchIdPage.empty(page, pageSize);
            }
            long total = searchClient.count(query);
            List<String> ids = searchClient.searchIds(query);
            return new SearchIdPage(ids, total, page, pageSize);
        }
    }

    /**
     * 将条件排序或默认 SQL ORDER BY 转为 ES 排序。
     *
     * @param condition    条件，可空
     * @param defaultOrder 默认 ORDER BY
     * @param mapping      字段映射
     * @return 排序列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static List<SearchSort> resolveSorts(AbstractCondition condition,
                                                String defaultOrder,
                                                SearchFieldMapping mapping) {
        SearchFieldMapping map = mapping == null ? SearchFieldMapping.create() : mapping;
        List<SearchSort> result = new ArrayList<>();
        if (condition != null) {
            List<SortColumn> columns = condition.getSortList();
            if (columns != null && !columns.isEmpty()) {
                for (SortColumn column : columns) {
                    if (column == null || !StringUtils.hasText(column.getColumn())) {
                        continue;
                    }
                    boolean asc = column.getType() == null || column.getType() == SortType.ASC;
                    result.add(new SearchSort(map.resolve(column.getColumn()), asc));
                }
                return result;
            }
        }
        return parseOrderBy(defaultOrder, map);
    }

    /**
     * 解析 SQL 风格 ORDER BY。
     *
     * @param orderBy ORDER BY 文本
     * @param mapping 字段映射
     * @return 排序列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static List<SearchSort> parseOrderBy(String orderBy, SearchFieldMapping mapping) {
        List<SearchSort> result = new ArrayList<>();
        if (!StringUtils.hasText(orderBy)) {
            return result;
        }
        SearchFieldMapping map = mapping == null ? SearchFieldMapping.create() : mapping;
        String[] parts = orderBy.split(",");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String[] tokens = part.trim().replace('\t', ' ').split("\\s+");
            if (tokens.length == 0 || !StringUtils.hasText(tokens[0])) {
                continue;
            }
            String column = tokens[0].trim();
            boolean asc = true;
            if (tokens.length >= 2) {
                String dir = tokens[1].trim().toUpperCase(Locale.ROOT);
                asc = !SortType.DESC.name().equals(dir);
            }
            result.add(new SearchSort(map.resolve(column), asc));
        }
        return result;
    }
}
