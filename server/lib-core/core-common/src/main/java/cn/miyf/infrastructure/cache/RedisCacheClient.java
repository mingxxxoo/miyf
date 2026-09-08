package cn.miyf.infrastructure.cache;

import cn.miyf.infrastructure.cache.support.CacheViews;
import cn.miyf.infrastructure.cache.support.JsonRedisCacheStore;
import cn.miyf.infrastructure.cache.support.RedisJsonValueCache;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/**
 * 基于 Lettuce（{@link org.springframework.data.redis.core.StringRedisTemplate}）的缓存门面实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public class RedisCacheClient implements CacheClient {

    private final JsonRedisCacheStore store;

    /**
     * 构造 Redis 缓存门面。
     *
     * @param store JSON 存储内核
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public RedisCacheClient(JsonRedisCacheStore store) {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean isEnabled() {
        return store.isEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> ObjectCache<T> objects(Class<T> type) {
        ValueCache<T> cache = new RedisJsonValueCache<>(store, store.objectType(type));
        return CacheViews.asObject(cache);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <E> ListCache<E> lists(Class<E> elementType) {
        ValueCache<List<E>> cache = new RedisJsonValueCache<>(store, store.listType(elementType));
        return CacheViews.asList(cache);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <N> TreeCache<N> trees(Class<N> nodeType) {
        // 树以「根节点列表」序列化，节点内嵌 children
        ValueCache<List<N>> cache = new RedisJsonValueCache<>(store, store.listType(nodeType));
        return CacheViews.asTree(cache);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> ValueCache<T> values(TypeReference<T> type) {
        return new RedisJsonValueCache<>(store, store.constructType(type));
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

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void evictByPrefix(String prefix) {
        store.evictByPrefix(prefix);
    }
}
