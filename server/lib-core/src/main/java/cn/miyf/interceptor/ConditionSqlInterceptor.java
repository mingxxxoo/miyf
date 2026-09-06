package cn.miyf.interceptor;

import cn.miyf.common.query.ConditionSqlRewriter;
import cn.miyf.common.query.AbstractCondition;
import cn.miyf.common.query.QueryConditionHolder;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 * MyBatis SQL 拦截器：根据 {@link QueryConditionHolder} / 参数中的 {@link AbstractCondition}
 * 动态注入排序与聚合分析，无需在 XML 写死 ORDER BY。
 * <p>
 * Mapper SQL 需包含标记 {@link ConditionSqlRewriter#ORDER_MARKER}。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:50
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class
        })
})
public class ConditionSqlInterceptor implements Interceptor {

    /**
     * 拦截查询并改写 BoundSql。
     *
     * @param invocation 调用
     * @return 查询结果
     * @throws Throwable 反射/SQL 异常
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        QueryConditionHolder.Context context = resolveContext(parameter);
        BoundSql boundSql = args.length == 6 ? (BoundSql) args[5] : ms.getBoundSql(parameter);
        String original = boundSql.getSql();
        if (context == null || original == null
                || (!original.contains(ConditionSqlRewriter.ORDER_MARKER)
                && !original.contains(ConditionSqlRewriter.AGGREGATE_MARKER))) {
            return invocation.proceed();
        }

        String rewritten = ConditionSqlRewriter.rewrite(original, context);
        if (rewritten.equals(original)) {
            return invocation.proceed();
        }

        setSql(boundSql, rewritten);
        // 四参数 query 会再次 getBoundSql，需用固定 SqlSource 包住已改写的 BoundSql
        MappedStatement newMs = mappedStatementWithBoundSql(ms, boundSql);
        args[0] = newMs;
        if (args.length == 6) {
            args[5] = boundSql;
        }
        return invocation.proceed();
    }

    private QueryConditionHolder.Context resolveContext(Object parameter) {
        QueryConditionHolder.Context holder = QueryConditionHolder.get();
        if (holder != null) {
            return holder;
        }
        AbstractCondition condition = findCondition(parameter);
        if (condition == null) {
            return null;
        }
        return QueryConditionHolder.Context.of(condition, null, null);
    }

    private AbstractCondition findCondition(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof AbstractCondition condition) {
            return condition;
        }
        if (parameter instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof AbstractCondition condition) {
                    return condition;
                }
            }
        }
        return null;
    }

    private void setSql(BoundSql boundSql, String sql) throws Exception {
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, sql);
    }

    private MappedStatement mappedStatementWithBoundSql(MappedStatement ms, BoundSql boundSql) {
        SqlSource sqlSource = parameterObject -> boundSql;
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId(), sqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        if (ms.getKeyColumns() != null && ms.getKeyColumns().length > 0) {
            builder.keyColumn(String.join(",", ms.getKeyColumns()));
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    @Override
    public void setProperties(Properties properties) {
        // 无额外配置
    }
}
