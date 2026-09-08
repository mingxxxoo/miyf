package cn.miyf.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 平台缓存门面（对齐 SearchClient 风格）。
 * 按形态提供对象 / 列表 / 树缓存视图，并支持按前缀批量失效；底层默认 Lettuce。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface CacheClient {

    /**
     * 是否启用真实 Redis 缓存。
     * Noop 实现返回 false，业务可据此决定是否跳过主动失效等逻辑。
     *
     * @return true 表示已连接 Redis 缓存
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    boolean isEnabled();

    /**
     * 获取单对象缓存视图。
     *
     * @param type 对象类型
     * @param <T>  类型
     * @return ObjectCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <T> ObjectCache<T> objects(Class<T> type);

    /**
     * 获取元素列表缓存视图。
     *
     * @param elementType 元素类型
     * @param <E>         元素类型
     * @return ListCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <E> ListCache<E> lists(Class<E> elementType);

    /**
     * 获取树形缓存视图（根列表 + 嵌套 children）。
     *
     * @param nodeType 节点类型
     * @param <N>      节点类型
     * @return TreeCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <N> TreeCache<N> trees(Class<N> nodeType);

    /**
     * 获取任意 Jackson 类型缓存视图（复杂泛型用 TypeReference）。
     *
     * @param type 类型引用
     * @param <T>  值类型
     * @return ValueCache
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    <T> ValueCache<T> values(TypeReference<T> type);

    /**
     * 删除单个缓存键。
     *
     * @param key 完整 key
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void evict(String key);

    /**
     * 按前缀 SCAN 删除，避免 KEYS 阻塞。
     *
     * @param prefix key 前缀
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void evictByPrefix(String prefix);
}
