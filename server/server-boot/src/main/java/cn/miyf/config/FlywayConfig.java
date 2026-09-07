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
     * <p>
     * 启动前先 {@code repair} 对齐 checksum，再 {@code migrate}，避免已应用脚本内容变更后启动失败。
     *
     * @param dataSource 数据源
     * @return Flyway 实例
     * @history 1.00 2026-09-04 17:22 XieMingJie Created.
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/postgresql")
                .baselineOnMigrate(true)
                // 关闭占位符替换，避免 SQL 注释/模板中的 ${var} 被误解析
                .placeholderReplacement(false)
                .load();
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}
