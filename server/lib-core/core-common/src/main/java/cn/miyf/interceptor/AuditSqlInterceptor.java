package cn.miyf.interceptor;

import cn.miyf.common.query.AuditSqlRewriter;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * MyBatis SQL 拦截器：手写 XML {@code INSERT}/{@code UPDATE} 自动补审计时间列。
 * <p>
 * MyBatis-Plus {@code insert}/{@code updateById} 已由 {@link cn.miyf.config.AuditMetaObjectHandler}
 * 填充实体字段，生成 SQL 通常已含对应列，本拦截器会跳过，避免重复赋值。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {
                MappedStatement.class, Object.class
        })
})
public class AuditSqlInterceptor implements Interceptor {

    /**
     * 拦截新增/更新并按需改写 BoundSql。
     *
     * @param invocation 调用
     * @return 影响行数
     * @throws Throwable 反射/SQL 异常
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        SqlCommandType commandType = ms.getSqlCommandType();
        if (commandType != SqlCommandType.UPDATE && commandType != SqlCommandType.INSERT) {
            return invocation.proceed();
        }

        Object parameter = args[1];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String original = boundSql.getSql();
        String rewritten = AuditSqlRewriter.rewrite(original);
        if (rewritten.equals(original)) {
            return invocation.proceed();
        }

        setSql(boundSql, rewritten);
        args[0] = mappedStatementWithBoundSql(ms, boundSql);
        return invocation.proceed();
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
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void setProperties(Properties properties) {
        // 无额外配置
    }
}
