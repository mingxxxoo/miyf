package cn.miyf.infrastructure.cache.support;

import cn.miyf.infrastructure.cache.ListCache;
import cn.miyf.infrastructure.cache.ObjectCache;
import cn.miyf.infrastructure.cache.TreeCache;
import cn.miyf.infrastructure.cache.ValueCache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 将 {@link ValueCache} 适配为对象 / 列表 / 树语义视图的委托实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class CacheViews {

    private CacheViews() {
    }

    /**
     * 对象缓存视图。
     *
     * @param delegate 底层值缓存
     * @param <T>      对象类型
     * @return ObjectCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T> ObjectCache<T> asObject(ValueCache<T> delegate) {
        return new ObjectCache<>() {
            @Override
            public Optional<T> get(String key) {
                return delegate.get(key);
            }

            @Override
            public void put(String key, T value, Duration ttl) {
                delegate.put(key, value, ttl);
            }

            @Override
            public T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
                return delegate.getOrLoad(key, ttl, loader);
            }

            @Override
            public void evict(String key) {
                delegate.evict(key);
            }
        };
    }

    /**
     * 列表缓存视图。
     *
     * @param delegate 底层值缓存（值为 List）
     * @param <E>      元素类型
     * @return ListCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <E> ListCache<E> asList(ValueCache<List<E>> delegate) {
        return new ListCache<>() {
            @Override
            public Optional<List<E>> get(String key) {
                return delegate.get(key);
            }

            @Override
            public void put(String key, List<E> value, Duration ttl) {
                delegate.put(key, value, ttl);
            }

            @Override
            public List<E> getOrLoad(String key, Duration ttl, Supplier<List<E>> loader) {
                return delegate.getOrLoad(key, ttl, loader);
            }

            @Override
            public void evict(String key) {
                delegate.evict(key);
            }
        };
    }

    /**
     * 树形缓存视图（根列表）。
     *
     * @param delegate 底层值缓存（值为根节点 List）
     * @param <N>      节点类型
     * @return TreeCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <N> TreeCache<N> asTree(ValueCache<List<N>> delegate) {
        return new TreeCache<>() {
            @Override
            public Optional<List<N>> get(String key) {
                return delegate.get(key);
            }

            @Override
            public void put(String key, List<N> value, Duration ttl) {
                delegate.put(key, value, ttl);
            }

            @Override
            public List<N> getOrLoad(String key, Duration ttl, Supplier<List<N>> loader) {
                return delegate.getOrLoad(key, ttl, loader);
            }

            @Override
            public void evict(String key) {
                delegate.evict(key);
            }
        };
    }
}
