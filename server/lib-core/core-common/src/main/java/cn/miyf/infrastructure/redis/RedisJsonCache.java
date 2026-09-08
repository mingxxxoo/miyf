package cn.miyf.infrastructure.redis;

import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.infrastructure.cache.ValueCache;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 兼容门面：历史 JSON 缓存 API，内部委托 {@link CacheClient}。
 * 新代码请直接注入 {@link CacheClient}，按对象 / 列表 / 树形态使用。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
public class RedisJsonCache {

    private final CacheClient cacheClient;

    /**
     * 构造兼容缓存组件。
     *
     * @param cacheClient 抽象缓存门面
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public RedisJsonCache(CacheClient cacheClient) {
        this.cacheClient = cacheClient;
    }

    /**
     * 读取缓存；未命中则加载并回填。
     *
     * @param key        缓存键
     * @param type       类型
     * @param ttlSeconds 正常 TTL（秒）
     * @param loader     加载器
     * @param <T>        类型
     * @return 值（可为 null，取决于 loader）
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, long ttlSeconds, Supplier<T> loader) {
        ValueCache<T> cache = cacheClient.values(type);
        return cache.getOrLoad(key, Duration.ofSeconds(Math.max(ttlSeconds, 1L)), loader);
    }

    /**
     * 删除单个 key。
     *
     * @param key 键
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void delete(String key) {
        cacheClient.evict(key);
    }

    /**
     * 按前缀 SCAN 删除（避免 KEYS）。
     *
     * @param prefix 前缀
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void deleteByPrefix(String prefix) {
        cacheClient.evictByPrefix(prefix);
    }

    /**
     * 读取原始字符串值（兼容旧调用；新代码请用形态化 Cache）。
     *
     * @param key 键
     * @return 可选值；Noop 或未命中为空
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public Optional<String> getRaw(String key) {
        return cacheClient.values(new TypeReference<String>() {
        }).get(key);
    }
}
