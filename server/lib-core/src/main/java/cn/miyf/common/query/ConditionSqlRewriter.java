package cn.miyf.common.query;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 QO 的 SQL 改写：排序与聚合分析，避免在 Mapper XML 写死 ORDER BY / 聚合。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:50
 */
public final class ConditionSqlRewriter {

    /** Mapper XML 中放置动态排序的标记 */
    public static final String ORDER_MARKER = "/* @conditionSql */";

    /** Mapper XML 中放置动态聚合 SELECT 片段的标记（可选） */
    public static final String AGGREGATE_MARKER = "/* @conditionAggregate */";

    private static final Pattern LIMIT_PATTERN = Pattern.compile(
            "(?i)\\sLIMIT\\s+", Pattern.MULTILINE);
    private static final Pattern ORDER_BY_TAIL = Pattern.compile(
            "(?i)\\sORDER\\s+BY\\s+[^;]*?(?=(\\sLIMIT\\s+|\\sOFFSET\\s+|\\sFETCH\\s+|$))", Pattern.DOTALL);
    private static final Set<String> AGG_TYPES = Set.of("SUM", "COUNT", "AVG", "MAX", "MIN");

    private ConditionSqlRewriter() {
    }

    /**
     * 改写 SQL：注入排序；在仅分析模式下包裹聚合查询。
     *
     * @param originalSql 原始 SQL
     * @param context     查询上下文，可空
     * @return 改写后 SQL；无需改写则返回原文
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static String rewrite(String originalSql, QueryConditionHolder.Context context) {
        if (originalSql == null || originalSql.isBlank() || context == null) {
            return originalSql;
        }
        if (!originalSql.contains(ORDER_MARKER) && !originalSql.contains(AGGREGATE_MARKER)) {
            return originalSql;
        }

        AbstractCondition condition = context.getCondition();
        List<ConditionAggregate> aggregates = resolveAggregates(condition);
        context.setLastAggregates(aggregates);

        boolean aggregateOnly = condition != null && !condition.isQueryRecord() && !aggregates.isEmpty();
        if (aggregateOnly) {
            return rewriteAsAggregate(originalSql, aggregates, context);
        }

        String sql = originalSql;
        if (sql.contains(AGGREGATE_MARKER)) {
            // 明细查询忽略聚合片段标记
            sql = sql.replace(AGGREGATE_MARKER, "");
        }
        if (sql.contains(ORDER_MARKER)) {
            String orderBy = buildOrderByClause(condition, context.getDefaultOrder(), context.allowedColumnSet());
            sql = sql.replace(ORDER_MARKER, orderBy.isEmpty() ? "" : " ORDER BY " + orderBy + " ");
        }
        return sql;
    }

    /**
     * 构造 ORDER BY 子句内容（不含 ORDER BY 关键字）。
     *
     * @param condition      QO
     * @param defaultOrder   默认排序原文
     * @param allowedColumns 白名单（小写），空则只做标识符校验
     * @return 排序片段
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static String buildOrderByClause(AbstractCondition condition,
                                            String defaultOrder,
                                            Set<String> allowedColumns) {
        List<SortColumn> sortColumns = condition == null ? List.of() : safeSortList(condition);
        if (!sortColumns.isEmpty()) {
            List<String> parts = new ArrayList<>(sortColumns.size());
            for (SortColumn col : sortColumns) {
                String column = assertSafeColumn(col.getColumn(), allowedColumns);
                SortType type = col.getType() == null ? SortType.ASC : col.getType();
                parts.add(column + " " + type.name());
            }
            return String.join(", ", parts);
        }
        if (defaultOrder == null || defaultOrder.isBlank()) {
            return "";
        }
        return normalizeDefaultOrder(defaultOrder, allowedColumns);
    }

    /**
     * 构造聚合 SELECT 列表，如 {@code SUM(rating) AS rating_sum}。
     *
     * @param aggregates     聚合定义
     * @param allowedColumns 白名单
     * @return SELECT 列表
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static String buildAggregateSelectList(List<ConditionAggregate> aggregates,
                                                   Set<String> allowedColumns) {
        if (aggregates == null || aggregates.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "聚合分析参数不能为空");
        }
        List<String> parts = new ArrayList<>(aggregates.size());
        for (ConditionAggregate agg : aggregates) {
            if (agg == null || agg.getType() == null || agg.getField() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "聚合参数不完整");
            }
            String type = agg.getType().trim().toUpperCase(Locale.ROOT);
            if (!AGG_TYPES.contains(type)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的聚合类型: " + agg.getType());
            }
            String field = agg.getField().trim();
            String aliasBase;
            if ("COUNT".equals(type) && ("*".equals(field) || "1".equals(field))) {
                aliasBase = "cnt";
                parts.add("COUNT(1) AS " + aliasBase);
                continue;
            }
            String safeField = assertSafeColumn(field, allowedColumns);
            aliasBase = safeField.replace('.', '_') + "_" + type.toLowerCase(Locale.ROOT);
            parts.add(type + "(" + safeField + ") AS " + aliasBase);
        }
        return String.join(", ", parts);
    }

    private static String rewriteAsAggregate(String originalSql,
                                             List<ConditionAggregate> aggregates,
                                             QueryConditionHolder.Context context) {
        String inner = originalSql;
        inner = inner.replace(ORDER_MARKER, "");
        inner = inner.replace(AGGREGATE_MARKER, "");
        inner = stripOrderByLimitOffset(inner);
        String selectList = buildAggregateSelectList(aggregates, context.allowedColumnSet());
        return "SELECT " + selectList + " FROM (" + inner + ") _cond_agg";
    }

    private static List<ConditionAggregate> resolveAggregates(AbstractCondition condition) {
        if (condition == null) {
            return List.of();
        }
        return condition.statisticParamList();
    }

    private static List<SortColumn> safeSortList(AbstractCondition condition) {
        if (condition.getSort() == null || condition.getSort().isBlank()) {
            return List.of();
        }
        return condition.getSortList();
    }

    private static String normalizeDefaultOrder(String defaultOrder, Set<String> allowedColumns) {
        String[] segments = defaultOrder.split(",");
        List<String> parts = new ArrayList<>(segments.length);
        for (String segment : segments) {
            String trimmed = segment.trim().replaceAll("\\s+", " ");
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] tokens = trimmed.split(" ");
            if (tokens.length == 1) {
                parts.add(assertSafeColumn(tokens[0], allowedColumns) + " ASC");
            } else {
                String column = assertSafeColumn(tokens[0], allowedColumns);
                String dir = tokens[1].trim().toUpperCase(Locale.ROOT);
                if (!SortType.ASC.name().equals(dir) && !SortType.DESC.name().equals(dir)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, AbstractCondition.ORDER_FIELD_ERROR_MSG);
                }
                parts.add(column + " " + dir);
            }
        }
        return String.join(", ", parts);
    }

    private static String assertSafeColumn(String column, Set<String> allowedColumns) {
        if (column == null || column.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, AbstractCondition.ORDER_FIELD_ERROR_MSG);
        }
        String trimmed = column.trim();
        if (!trimmed.matches("[a-zA-Z0-9_\\.]+")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, AbstractCondition.ORDER_FIELD_ERROR_MSG);
        }
        if (allowedColumns != null && !allowedColumns.isEmpty()
                && !allowedColumns.contains(trimmed.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, AbstractCondition.ORDER_FIELD_ERROR_MSG);
        }
        return trimmed;
    }

    private static String stripOrderByLimitOffset(String sql) {
        String result = sql;
        Matcher orderMatcher = ORDER_BY_TAIL.matcher(result);
        result = orderMatcher.replaceAll(" ");
        // 去掉末尾 LIMIT/OFFSET，聚合分析不需要分页
        Matcher limitMatcher = LIMIT_PATTERN.matcher(result);
        if (limitMatcher.find()) {
            result = result.substring(0, limitMatcher.start());
        }
        return result.trim();
    }
}
