package cn.miyf.infrastructure.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * HikariCP 数据源工厂：每个数据源独立连接池，禁止多库共用同一池。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Component
public class DataSourceFactory {

    /**
     * 按配置项创建独立 {@link HikariDataSource}。
     *
     * @param item 数据源配置
     * @return Hikari 数据源
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public DataSource create(DataSourceProperties.DataSourceItem item) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(item.getUrl());
        config.setUsername(item.getUsername());
        config.setPassword(item.getPassword());
        config.setDriverClassName(item.getDriverClassName());
        config.setMinimumIdle(item.getMinimumIdle());
        config.setMaximumPoolSize(item.getMaximumPoolSize());
        config.setPoolName(item.getPoolName());
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }
}
