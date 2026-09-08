package cn.miyf.infrastructure.cache;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 树形缓存：以「根节点列表」为缓存单元，节点内嵌 children 形成整棵/多棵树。
 * 适合菜单树、组织树、权限树等一次加载多次读的场景。
 *
 * @param <N> 树节点类型（通常含 {@code List<N> children}）
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface TreeCache<N> extends ValueCache<List<N>> {

    /**
     * 读取或加载整棵树（根列表）。
     *
     * @param treeKey 树缓存键（可按用户/产品等维度区分）
     * @param ttl     正常 TTL
     * @param loader  树加载器
     * @return 根节点列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default List<N> getOrLoadTree(String treeKey, Duration ttl, Supplier<List<N>> loader) {
        return getOrLoad(treeKey, ttl, loader);
    }

    /**
     * 写入整棵树。
     *
     * @param treeKey 树缓存键
     * @param roots   根节点列表
     * @param ttl     正常 TTL
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default void putTree(String treeKey, List<N> roots, Duration ttl) {
        put(treeKey, roots, ttl);
    }

    /**
     * 失效指定树缓存。
     *
     * @param treeKey 树缓存键
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default void evictTree(String treeKey) {
        evict(treeKey);
    }
}
