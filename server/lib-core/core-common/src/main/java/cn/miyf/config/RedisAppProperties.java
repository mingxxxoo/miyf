package cn.miyf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 缓存 / 限流 / 分布式锁配置（前缀 {@code app.redis}）。
 * 连接客户端为 Lettuce，连接参数见 {@code spring.data.redis.*} 与 {@code spring.data.redis.lettuce.*}。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@ConfigurationProperties(prefix = "app.redis")
public class RedisAppProperties {

    /**
     * 总开关；false 时降级为直查数据库、不限流、锁直接执行，并注入 Noop CacheClient。
     */
    private boolean enabled = true;

    private final Cache cache = new Cache();
    private final RateLimit rateLimit = new RateLimit();
    private final Lock lock = new Lock();

    /**
     * @return 是否启用 Redis
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return 缓存 TTL 配置
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * @return 限流配置
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * @return 分布式锁配置
     */
    public Lock getLock() {
        return lock;
    }

    /**
     * 业务缓存 TTL。
     */
    public static class Cache {
        /** 分类列表 TTL（秒）。 */
        private long categoryTtlSeconds = 600;
        /** 热门/推荐菜品 TTL（秒）。 */
        private long hotDishTtlSeconds = 300;
        /** 空结果防穿透 TTL（秒）。 */
        private long emptyTtlSeconds = 60;
        /** 树形数据默认 TTL（秒），如菜单/组织树。 */
        private long treeTtlSeconds = 600;
        /** 单对象默认 TTL（秒）。 */
        private long objectTtlSeconds = 300;

        public long getCategoryTtlSeconds() {
            return categoryTtlSeconds;
        }

        public void setCategoryTtlSeconds(long categoryTtlSeconds) {
            this.categoryTtlSeconds = categoryTtlSeconds;
        }

        public long getHotDishTtlSeconds() {
            return hotDishTtlSeconds;
        }

        public void setHotDishTtlSeconds(long hotDishTtlSeconds) {
            this.hotDishTtlSeconds = hotDishTtlSeconds;
        }

        public long getEmptyTtlSeconds() {
            return emptyTtlSeconds;
        }

        public void setEmptyTtlSeconds(long emptyTtlSeconds) {
            this.emptyTtlSeconds = emptyTtlSeconds;
        }

        public long getTreeTtlSeconds() {
            return treeTtlSeconds;
        }

        public void setTreeTtlSeconds(long treeTtlSeconds) {
            this.treeTtlSeconds = treeTtlSeconds;
        }

        public long getObjectTtlSeconds() {
            return objectTtlSeconds;
        }

        public void setObjectTtlSeconds(long objectTtlSeconds) {
            this.objectTtlSeconds = objectTtlSeconds;
        }
    }

    /**
     * 限流配置。
     */
    public static class RateLimit {
        private boolean enabled = true;
        /** 登录失败窗口内最大尝试次数。 */
        private int loginMaxAttempts = 10;
        /** 登录失败计数窗口（秒）。 */
        private long loginWindowSeconds = 300;
        /** 通用 API 窗口内最大请求数。 */
        private int apiMaxRequests = 120;
        /** 通用 API 窗口（秒）。 */
        private long apiWindowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public long getLoginWindowSeconds() {
            return loginWindowSeconds;
        }

        public void setLoginWindowSeconds(long loginWindowSeconds) {
            this.loginWindowSeconds = loginWindowSeconds;
        }

        public int getApiMaxRequests() {
            return apiMaxRequests;
        }

        public void setApiMaxRequests(int apiMaxRequests) {
            this.apiMaxRequests = apiMaxRequests;
        }

        public long getApiWindowSeconds() {
            return apiWindowSeconds;
        }

        public void setApiWindowSeconds(long apiWindowSeconds) {
            this.apiWindowSeconds = apiWindowSeconds;
        }
    }

    /**
     * 分布式锁配置。
     */
    public static class Lock {
        private long defaultLeaseSeconds = 10;

        public long getDefaultLeaseSeconds() {
            return defaultLeaseSeconds;
        }

        public void setDefaultLeaseSeconds(long defaultLeaseSeconds) {
            this.defaultLeaseSeconds = defaultLeaseSeconds;
        }
    }
}
