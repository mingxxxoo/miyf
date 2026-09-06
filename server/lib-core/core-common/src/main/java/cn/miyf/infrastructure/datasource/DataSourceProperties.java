package cn.miyf.infrastructure.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多数据源配置属性，绑定前缀 {@code app.datasource}。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceProperties {

    private DataSourceItem primary = new DataSourceItem();
    private DataSourceItem secondary = new DataSourceItem();

    /**
     * 获取主数据源配置。
     *
     * @return 主数据源项
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public DataSourceItem getPrimary() {
        return primary;
    }

    /**
     * 设置主数据源配置。
     *
     * @param primary 主数据源项
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public void setPrimary(DataSourceItem primary) {
        this.primary = primary;
    }

    /**
     * 获取次数据源配置。
     *
     * @return 次数据源项
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public DataSourceItem getSecondary() {
        return secondary;
    }

    /**
     * 设置次数据源配置。
     *
     * @param secondary 次数据源项
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public void setSecondary(DataSourceItem secondary) {
        this.secondary = secondary;
    }

    /**
     * 单个数据源连接与连接池参数。
     *
     * @author XieMingJie
     * @since 2026-09-04 16:41
     */
    public static class DataSourceItem {
        private boolean enabled = true;
        private String type = "postgresql";
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private int initialSize = 2;
        private int minIdle = 2;
        private int maxActive = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public int getInitialSize() {
            return initialSize;
        }

        public void setInitialSize(int initialSize) {
            this.initialSize = initialSize;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public int getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }
    }
}
