package cn.miyf.infrastructure.cache.support;

import cn.miyf.infrastructure.cache.ValueCache;
import com.fasterxml.jackson.databind.JavaType;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 基于 {@link JsonRedisCacheStore} 的通用 {@link ValueCache} 实现。
 *
 * @param <T> 值类型
 * @author XieMingJie
 * @since 2026-09-08
 */
@RequiredArgsConstructor
public class RedisJsonValueCache<T> implements ValueCache<T> {

    private final JsonRedisCacheStore store;
    private final JavaType javaType;

    
    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<T> get(String key) {
        return store.get(key, javaType);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void put(String key, T value, Duration ttl) {
        store.put(key, value, ttl);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        return store.getOrLoad(key, javaType, ttl, loader);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void evict(String key) {
        store.evict(key);
    }
}
