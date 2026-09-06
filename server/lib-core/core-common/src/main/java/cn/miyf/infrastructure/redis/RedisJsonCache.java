package cn.miyf.infrastructure.redis;

import cn.miyf.config.RedisAppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * JSON 缓存读写：支持空值短 TTL 防穿透；Redis 不可用时降级直查。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
public class RedisJsonCache {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);
    private static final String NULL_MARKER = "__NULL__";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisAppProperties properties;

    /**
     * 构造缓存组件。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper        JSON
     * @param properties          配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public RedisJsonCache(StringRedisTemplate stringRedisTemplate,
                          ObjectMapper objectMapper,
                          RedisAppProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 读取缓存；未命中则加载并回填。
     *
     * @param key        缓存键
     * @param type       类型
     * @param ttlSeconds 正常 TTL
     * @param loader     加载器
     * @param <T>        类型
     * @return 值（可为 null，取决于 loader）
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, long ttlSeconds, Supplier<T> loader) {
        if (!properties.isEnabled()) {
            return loader.get();
        }
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (NULL_MARKER.equals(cached)) {
                    return null;
                }
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception ex) {
            log.warn("Redis get failed, fallback to loader. key={}", key, ex);
            return loader.get();
        }

        T value = loader.get();
        try {
            if (value == null) {
                stringRedisTemplate.opsForValue().set(
                        key, NULL_MARKER, properties.getCache().getEmptyTtlSeconds(), TimeUnit.SECONDS);
            } else {
                String json = objectMapper.writeValueAsString(value);
                // 空集合也写短 TTL，减轻穿透
                boolean emptyCollection = value instanceof java.util.Collection<?> c && c.isEmpty();
                long ttl = emptyCollection ? properties.getCache().getEmptyTtlSeconds() : ttlSeconds;
                stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(Math.max(ttl, 1)));
            }
        } catch (Exception ex) {
            log.warn("Redis put failed, ignore. key={}", key, ex);
        }
        return value;
    }

    /**
     * 删除单个 key。
     *
     * @param key 键
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void delete(String key) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Redis delete failed. key={}", key, ex);
        }
    }

    /**
     * 按前缀 SCAN 删除（避免 KEYS）。
     *
     * @param prefix 前缀
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void deleteByPrefix(String prefix) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis deleteByPrefix failed. prefix={}", prefix, ex);
        }
    }

    /**
     * 读取原始字符串值。
     *
     * @param key 键
     * @return 可选值
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public Optional<String> getRaw(String key) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
        } catch (Exception ex) {
            log.warn("Redis getRaw failed. key={}", key, ex);
            return Optional.empty();
        }
    }
}

