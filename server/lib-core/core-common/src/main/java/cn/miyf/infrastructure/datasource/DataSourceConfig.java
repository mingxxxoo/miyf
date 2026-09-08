package cn.miyf.infrastructure.datasource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源与事务装配。
 * <p>
 * 强制 PRIMARY 启用；SECONDARY 仅在 enabled 且 URL 非空时注册。
 * Flyway 迁移由业务模块自行装配。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    /**
     * 创建路由数据源并注册到 {@link DataSourceRegistry}。
     *
     * @param properties 配置
     * @param factory    HikariCP 工厂
     * @param registry   注册表
     * @return 路由 DataSource（Primary Bean）
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     * @history 1.01 2026-09-08 XieMingJie 连接池改为 HikariCP。
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties,
                                 DataSourceFactory factory,
                                 DataSourceRegistry registry) {
        if (!properties.getPrimary().isEnabled()) {
            // 主库是业务硬依赖，关闭则无法启动
            throw new IllegalStateException("PRIMARY datasource must be enabled");
        }
        DataSource primary = factory.create(properties.getPrimary());
        registry.register(DataSourceKey.PRIMARY, primary);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceKey.PRIMARY, primary);

        // SECONDARY 为架构预留，未配置 URL 时不创建连接池
        if (properties.getSecondary().isEnabled()
                && properties.getSecondary().getUrl() != null
                && !properties.getSecondary().getUrl().isBlank()) {
            DataSource secondary = factory.create(properties.getSecondary());
            registry.register(DataSourceKey.SECONDARY, secondary);
            targetDataSources.put(DataSourceKey.SECONDARY, secondary);
        }

        DataSourceRouter router = new DataSourceRouter();
        router.setDefaultTargetDataSource(primary);
        router.setTargetDataSources(targetDataSources);
        router.afterPropertiesSet();
        return router;
    }

    /**
     * 单数据源事务管理器（当前版本不做跨库事务）。
     *
     * @param dataSource 路由数据源
     * @return 事务管理器
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}

