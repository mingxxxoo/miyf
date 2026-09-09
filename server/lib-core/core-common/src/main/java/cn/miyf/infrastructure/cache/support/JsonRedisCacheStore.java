package cn.miyf.infrastructure.cache.support;

import cn.miyf.config.RedisAppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Lettuce 字符串 JSON 缓存内核。
 * 统一空值占位、空集合短 TTL、异常降级直查与 SCAN 前缀删除；供各形态 ValueCache 复用。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@RequiredArgsConstructor
@Slf4j
public class JsonRedisCacheStore {

    /** 空值占位，防止缓存穿透。 */
    public static final String NULL_MARKER = "__NULL__";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisAppProperties properties;

    
    /**
     * @return 是否启用 Redis
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * @return 空结果 TTL（秒）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public long emptyTtlSeconds() {
        return properties.getCache().getEmptyTtlSeconds();
    }

    /**
     * 由 TypeReference 构造 JavaType。
     *
     * @param type 类型引用
     * @return JavaType
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public JavaType constructType(TypeReference<?> type) {
        return objectMapper.getTypeFactory().constructType(type);
    }

    /**
     * 构造普通对象 JavaType。
     *
     * @param type 类型
     * @return JavaType
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public JavaType objectType(Class<?> type) {
        return objectMapper.getTypeFactory().constructType(type);
    }

    /**
     * 构造 {@code List<E>} 的 JavaType。
     *
     * @param elementType 元素类型
     * @return JavaType
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public JavaType listType(Class<?> elementType) {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
    }

    /**
     * 按 JavaType 读取。
     *
     * @param key  键
     * @param type Jackson 类型
     * @param <T>  值类型
     * @return 可选值；未命中或空占位返回 empty
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public <T> Optional<T> get(String key, JavaType type) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached == null || NULL_MARKER.equals(cached)) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(cached, type));
        } catch (Exception ex) {
            log.warn("Redis get failed. key={}", key, ex);
            return Optional.empty();
        }
    }

    /**
     * 写入 JSON 值。
     *
     * @param key   键
     * @param value 值
     * @param ttl   正常 TTL
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void put(String key, Object value, Duration ttl) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            if (value == null) {
                stringRedisTemplate.opsForValue().set(
                        key, NULL_MARKER, emptyTtlSeconds(), TimeUnit.SECONDS);
                return;
            }
            String json = objectMapper.writeValueAsString(value);
            // 空集合使用短 TTL，减轻列表穿透
            boolean emptyCollection = value instanceof Collection<?> c && c.isEmpty();
            long seconds = emptyCollection
                    ? emptyTtlSeconds()
                    : Math.max(ttl == null ? emptyTtlSeconds() : ttl.getSeconds(), 1L);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(seconds));
        } catch (Exception ex) {
            log.warn("Redis put failed, ignore. key={}", key, ex);
        }
    }

    /**
     * 读取或加载并回填。
     *
     * @param key    键
     * @param type   Jackson 类型
     * @param ttl    正常 TTL
     * @param loader 加载器
     * @param <T>    值类型
     * @return 结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public <T> T getOrLoad(String key, JavaType type, Duration ttl, Supplier<T> loader) {
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
        put(key, value, ttl);
        return value;
    }

    /**
     * 删除单个键。
     *
     * @param key 键
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void evict(String key) {
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
     * 按前缀 SCAN 删除。
     *
     * @param prefix 前缀
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void evictByPrefix(String prefix) {
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
}
