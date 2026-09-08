package cn.miyf.common.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * 父子关系索引：不依赖节点 children 字段，适合按 ID 做无限极子孙查询（如数据范围）。
 *
 * @param <K> 主键 / 父编码类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class TreeRelationIndex<K> {

    private final Map<K, List<K>> childrenByParent;

    private TreeRelationIndex(Map<K, List<K>> childrenByParent) {
        this.childrenByParent = childrenByParent;
    }

    /**
     * 由扁平列表构建父子索引。
     *
     * @param items        扁平节点
     * @param idGetter     主键
     * @param parentGetter 父编码（可定制字段）
     * @param <T>          节点类型
     * @param <K>          键类型
     * @return 索引
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> TreeRelationIndex<K> of(Collection<T> items,
                                                 Function<? super T, ? extends K> idGetter,
                                                 Function<? super T, ? extends K> parentGetter) {
        Objects.requireNonNull(idGetter, "idGetter");
        Objects.requireNonNull(parentGetter, "parentGetter");
        Map<K, List<K>> childrenByParent = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return new TreeRelationIndex<>(childrenByParent);
        }
        for (T item : items) {
            if (item == null) {
                continue;
            }
            K id = idGetter.apply(item);
            if (id == null) {
                continue;
            }
            K parent = parentGetter.apply(item);
            if (parent == null) {
                continue;
            }
            childrenByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(id);
        }
        return new TreeRelationIndex<>(childrenByParent);
    }

    /**
     * 直接子节点 ID（不含自身）。
     *
     * @param parentId 父 ID
     * @return 子 ID 列表（只读视图的可变副本）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<K> childrenOf(K parentId) {
        List<K> kids = childrenByParent.get(parentId);
        return kids == null ? List.of() : List.copyOf(kids);
    }

    /**
     * 自身及全部子孙（BFS，深度不限）。
     *
     * @param rootId 根 ID
     * @return ID 集合；rootId 为 null 时返回空集
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Set<K> selfAndDescendants(K rootId) {
        Set<K> result = new HashSet<>();
        if (rootId == null) {
            return result;
        }
        Queue<K> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            K id = queue.poll();
            if (!result.add(id)) {
                continue;
            }
            List<K> kids = childrenByParent.get(id);
            if (kids != null) {
                queue.addAll(kids);
            }
        }
        return result;
    }
}
