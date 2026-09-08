package cn.miyf.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存关闭时的空实现：读操作直通 loader，写/失效为空操作。
 * 保证业务可注入 {@link CacheClient} 且不依赖 Redis 进程。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public class NoopCacheClient implements CacheClient {

    private static final Logger log = LoggerFactory.getLogger(NoopCacheClient.class);

    /**
     * 构造 Noop 门面并输出一次提示日志。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public NoopCacheClient() {
        log.info("CacheClient 使用 Noop 实现（app.redis.enabled=false）");
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean isEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> ObjectCache<T> objects(Class<T> type) {
        return new NoopValueCache<>();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <E> ListCache<E> lists(Class<E> elementType) {
        return new NoopListCache<>();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <N> TreeCache<N> trees(Class<N> nodeType) {
        return new NoopTreeCache<>();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public <T> ValueCache<T> values(TypeReference<T> type) {
        return new NoopValueCache<>();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void evict(String key) {
        // 关闭缓存时无需失效
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void evictByPrefix(String prefix) {
        // 关闭缓存时无需失效
    }

    /**
     * Noop 值缓存：get 恒 empty，getOrLoad 直通 loader。
     *
     * @param <T> 值类型
     */
    private static final class NoopValueCache<T> implements ValueCache<T>, ObjectCache<T> {
        @Override
        public Optional<T> get(String key) {
            return Optional.empty();
        }

        @Override
        public void put(String key, T value, Duration ttl) {
            // no-op
        }

        @Override
        public T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
            return loader.get();
        }

        @Override
        public void evict(String key) {
            // no-op
        }
    }

    /**
     * Noop 列表缓存。
     *
     * @param <E> 元素类型
     */
    private static final class NoopListCache<E> implements ListCache<E> {
        @Override
        public Optional<List<E>> get(String key) {
            return Optional.empty();
        }

        @Override
        public void put(String key, List<E> value, Duration ttl) {
            // no-op
        }

        @Override
        public List<E> getOrLoad(String key, Duration ttl, Supplier<List<E>> loader) {
            return loader.get();
        }

        @Override
        public void evict(String key) {
            // no-op
        }
    }

    /**
     * Noop 树缓存。
     *
     * @param <N> 节点类型
     */
    private static final class NoopTreeCache<N> implements TreeCache<N> {
        @Override
        public Optional<List<N>> get(String key) {
            return Optional.empty();
        }

        @Override
        public void put(String key, List<N> value, Duration ttl) {
            // no-op
        }

        @Override
        public List<N> getOrLoad(String key, Duration ttl, Supplier<List<N>> loader) {
            return loader.get();
        }

        @Override
        public void evict(String key) {
            // no-op
        }
    }
}
