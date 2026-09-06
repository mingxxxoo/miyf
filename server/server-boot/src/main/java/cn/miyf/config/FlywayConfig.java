package cn.miyf.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway 数据库迁移配置（业务库脚本位于 classpath:db/migration/postgresql）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:22
 */
@Configuration
public class FlywayConfig {

    /**
     * Flyway 迁移：仅加载 PostgreSQL 目录脚本。
     *
     * @param dataSource 数据源
     * @return Flyway 实例（启动时 migrate）
     * @history 1.00 2026-09-04 17:22 XieMingJie Created.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/postgresql")
                .baselineOnMigrate(true)
                .load();
    }
}
