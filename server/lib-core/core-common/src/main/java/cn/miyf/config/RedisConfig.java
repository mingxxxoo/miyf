package cn.miyf.config;

import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.infrastructure.cache.NoopCacheClient;
import cn.miyf.infrastructure.cache.RedisCacheClient;
import cn.miyf.infrastructure.cache.support.JsonRedisCacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis / 缓存模块装配。
 * 客户端固定为 Spring Data Redis 默认的 Lettuce（见 spring.data.redis.lettuce.*）；
 * 本配置注册 {@link CacheClient} 抽象门面：启用时走 Redis JSON 缓存，关闭时注入 Noop。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Configuration
@EnableConfigurationProperties(RedisAppProperties.class)
public class RedisConfig {

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
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(StringRedisTemplate.class)
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
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(JsonRedisCacheStore.class)
    public CacheClient redisCacheClient(JsonRedisCacheStore store) {
        return new RedisCacheClient(store);
    }

    /**
     * 关闭 Redis 时的 Noop 门面，保证业务仍可注入 CacheClient。
     *
     * @return CacheClient
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false")
    public CacheClient noopCacheClient() {
        return new NoopCacheClient();
    }
}
