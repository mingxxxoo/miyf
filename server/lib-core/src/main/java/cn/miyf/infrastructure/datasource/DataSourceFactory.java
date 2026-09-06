package cn.miyf.infrastructure.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Druid 数据源工厂：每个数据源独立连接池，禁止多库共用同一池。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Component
public class DataSourceFactory {

    /**
     * 按配置项创建独立 {@link DruidDataSource}。
     *
     * @param item 数据源配置
     * @return Druid 数据源
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public DataSource create(DataSourceProperties.DataSourceItem item) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(item.getUrl());
        dataSource.setUsername(item.getUsername());
        dataSource.setPassword(item.getPassword());
        dataSource.setDriverClassName(item.getDriverClassName());
        dataSource.setInitialSize(item.getInitialSize());
        dataSource.setMinIdle(item.getMinIdle());
        dataSource.setMaxActive(item.getMaxActive());
        // 空闲检测，避免失效连接进入业务
        dataSource.setTestWhileIdle(true);
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }
}
