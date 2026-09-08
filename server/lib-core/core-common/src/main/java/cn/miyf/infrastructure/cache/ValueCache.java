package cn.miyf.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 通用值缓存契约（对象 / 列表 / 树均基于此语义）。
 * 负责按 key 读写 JSON、未命中回源，以及单 key 失效。
 *
 * @param <T> 缓存值类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface ValueCache<T> {

    /**
     * 读取缓存值。
     *
     * @param key 缓存键
     * @return 命中时有值；未命中或空值占位返回 empty / 业务约定
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<T> get(String key);

    /**
     * 写入缓存。
     *
     * @param key   缓存键
     * @param value 值；null 时按空值短 TTL 防穿透
     * @param ttl   正常过期时间
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void put(String key, T value, Duration ttl);

    /**
     * 读取缓存；未命中则执行 loader 并回填。
     *
     * @param key    缓存键
     * @param ttl    正常 TTL
     * @param loader 数据加载器
     * @return 缓存或加载结果（可为 null）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    T getOrLoad(String key, Duration ttl, Supplier<T> loader);

    /**
     * 删除单个缓存键。
     *
     * @param key 缓存键
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void evict(String key);
}
