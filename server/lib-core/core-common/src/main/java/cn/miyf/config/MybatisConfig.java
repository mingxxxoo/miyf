package cn.miyf.config;

import cn.miyf.interceptor.ConditionSqlInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * MyBatis-Plus 配置：分页插件、条件 SQL 拦截器与自动填充。
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
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
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
     * @history 1.00 2026-09-04 17:50 XieMingJie Created.
     */
    @Bean
    public ConditionSqlInterceptor conditionSqlInterceptor() {
        return new ConditionSqlInterceptor();
    }

    /**
     * 插入/更新时间自动填充。
     *
     * @return MetaObjectHandler
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Instant now = Instant.now();
                strictInsertFill(metaObject, "createTime", Instant.class, now);
                strictInsertFill(metaObject, "lastModifyTime", Instant.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "lastModifyTime", Instant.class, Instant.now());
            }
        };
    }
}
