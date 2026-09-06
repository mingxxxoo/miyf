package cn.miyf.common.query;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 查询条件线程上下文：供 SQL 拦截器读取 QO 中的排序与聚合分析参数。
 * <p>
 * 在调用 Mapper 前 {@link #run} / {@link #set}，请求结束务必 {@link #clear()}。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:50
 */
public final class QueryConditionHolder {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private QueryConditionHolder() {
    }

    /**
     * 绑定条件并执行，结束自动清理。
     *
     * @param condition    QO，可空
     * @param defaultOrder 无 sort 时的默认排序，如 {@code created_at DESC}
     * @param action       业务动作
     * @param <T>          返回类型
     * @return 动作结果
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static <T> T run(AbstractCondition condition, String defaultOrder, Supplier<T> action) {
        set(condition, defaultOrder, null);
        try {
            return action.get();
        } finally {
            clear();
        }
    }

    /**
     * 绑定条件（含可选白名单列）。
     *
     * @param condition      QO
     * @param defaultOrder   默认排序
     * @param allowedColumns 允许排序/聚合的列，null 表示仅做标识符校验
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static void set(AbstractCondition condition, String defaultOrder, Set<String> allowedColumns) {
        CONTEXT.set(Context.of(condition, defaultOrder, allowedColumns));
    }

    /**
     * 获取当前上下文。
     *
     * @return 上下文，未绑定则为 null
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static Context get() {
        return CONTEXT.get();
    }

    /**
     * 清理线程上下文。
     *
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 线程上下文快照。
     *
     * @author XieMingJie
     * @since 2026-09-04 17:50
     */
    public static final class Context {
        private final AbstractCondition condition;
        private final String defaultOrder;
        private final Set<String> allowedColumns;
        private List<ConditionAggregate> lastAggregates = Collections.emptyList();

        private Context(AbstractCondition condition, String defaultOrder, Set<String> allowedColumns) {
            this.condition = condition;
            this.defaultOrder = defaultOrder;
            this.allowedColumns = allowedColumns == null ? null : Set.copyOf(allowedColumns);
        }

        /**
         * 构造上下文。
         *
         * @param condition      QO
         * @param defaultOrder   默认排序
         * @param allowedColumns 白名单
         * @return 上下文
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public static Context of(AbstractCondition condition, String defaultOrder, Set<String> allowedColumns) {
            return new Context(condition, defaultOrder, allowedColumns);
        }

        /**
         * @return QO
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public AbstractCondition getCondition() {
            return condition;
        }

        /**
         * @return 默认排序
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public String getDefaultOrder() {
            return defaultOrder;
        }

        /**
         * @return 列白名单
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public Set<String> getAllowedColumns() {
            return allowedColumns;
        }

        /**
         * 最近一次解析出的聚合定义（便于单测/调试）。
         *
         * @return 聚合列表
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public List<ConditionAggregate> getLastAggregates() {
            return lastAggregates;
        }

        void setLastAggregates(List<ConditionAggregate> lastAggregates) {
            this.lastAggregates = lastAggregates == null ? List.of() : List.copyOf(lastAggregates);
        }

        /**
         * 合并白名单为可变集合。
         *
         * @return 集合
         * @history 1.00 2026-09-04 17:50 XieMingJie Created.
         */
        public Set<String> allowedColumnSet() {
            if (allowedColumns == null || allowedColumns.isEmpty()) {
                return new LinkedHashSet<>();
            }
            Set<String> set = new LinkedHashSet<>();
            for (String col : allowedColumns) {
                if (col != null) {
                    set.add(col.toLowerCase(Locale.ROOT));
                }
            }
            return set;
        }
    }
}
