package cn.miyf.config;

import cn.miyf.interceptor.AuditSqlInterceptor;
import cn.miyf.interceptor.ConditionSqlInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页、条件 SQL、审计 SQL 拦截器。
 * 实体字段填充见 {@link AuditMetaObjectHandler}；手写 INSERT/UPDATE 见 {@link AuditSqlInterceptor}。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Configuration
public class MybatisConfig {

    /**
     * 分页拦截器（PostgreSQL）。
     *
     * @return 拦截器
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * QO 排序/聚合 SQL 拦截器（由 MyBatis-Plus 自动挂到 SqlSessionFactory）。
     *
     * @return 拦截器
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Bean
    public ConditionSqlInterceptor conditionSqlInterceptor() {
        return new ConditionSqlInterceptor();
    }

    /**
     * 手写 INSERT/UPDATE 自动补审计时间列。
     *
     * @return 拦截器
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    public AuditSqlInterceptor auditSqlInterceptor() {
        return new AuditSqlInterceptor();
    }
}
