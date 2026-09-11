package cn.miyf.config;

import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.infrastructure.cache.NoopCacheClient;
import cn.miyf.infrastructure.cache.RedisCacheClient;
import cn.miyf.infrastructure.cache.support.JsonRedisCacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis / 缓存模块装配。
 * 客户端固定为 Spring Data Redis 默认的 Lettuce（见 spring.data.redis.lettuce.*）；
 * 本配置注册 {@link CacheClient} 抽象门面：启用时走 Redis JSON 缓存，否则注入 Noop。
 * <p>
 * 勿在用户 {@code @Configuration} 上对 Boot 自动配置的 {@link StringRedisTemplate}
 * 使用 {@code @ConditionalOnBean}（用户配置早于自动配置解析，条件会误判为不存在）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Configuration
@EnableConfigurationProperties(RedisAppProperties.class)
public class RedisConfig {

    /**
     * {@code app.redis.enabled=true}（默认）时装配 Redis 缓存门面。
     */
    @Configuration
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class EnabledCacheConfiguration {

        /**
         * JSON 缓存存储内核（依赖 Boot 自动配置的 Lettuce StringRedisTemplate）。
         *
         * @param stringRedisTemplate Lettuce 字符串模板
         * @param objectMapper        Jackson
         * @param properties          应用 Redis 配置
         * @return JsonRedisCacheStore
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        @Bean
        public JsonRedisCacheStore jsonRedisCacheStore(StringRedisTemplate stringRedisTemplate,
                                                       ObjectMapper objectMapper,
                                                       RedisAppProperties properties) {
            return new JsonRedisCacheStore(stringRedisTemplate, objectMapper, properties);
        }

        /**
         * 启用 Redis 时的缓存门面。
         *
         * @param store 存储内核
         * @return CacheClient
         * @history 1.00 2026-09-08 XieMingJie Created.
         */
        @Bean
        public CacheClient redisCacheClient(JsonRedisCacheStore store) {
            return new RedisCacheClient(store);
        }
    }

    /**
     * 关闭 Redis，或启用路径未贡献 CacheClient 时的 Noop 门面（MissingBean 兜底）。
     *
     * @return CacheClient
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    @ConditionalOnMissingBean(CacheClient.class)
    public CacheClient noopCacheClient() {
        return new NoopCacheClient();
    }
}
