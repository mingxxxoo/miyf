package cn.miyf.common.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树工具门面：扁平列表 → 无限极树；支持定制父编码字段与同级排序。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 创建无限极树构建器。
     *
     * @param idGetter       主键（本节点编码）
     * @param parentGetter   父编码字段（如 getParentId / getParentCode）
     * @param childrenGetter 子节点读
     * @param childrenSetter 子节点写
     * @param <T>            节点类型
     * @param <K>            键类型
     * @return 构建器
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> TreeBuilder<T, K> builder(Function<T, K> idGetter,
                                                   Function<T, K> parentGetter,
                                                   Function<T, List<T>> childrenGetter,
                                                   BiConsumer<T, List<T>> childrenSetter) {
        return TreeBuilder.of(idGetter, parentGetter, childrenGetter, childrenSetter);
    }

    /**
     * 快捷建树：父为 null 为根，不排序，孤儿当根。
     *
     * @param nodes          扁平列表
     * @param idGetter       主键
     * @param parentGetter   父编码
     * @param childrenGetter 子读
     * @param childrenSetter 子写
     * @param <T>            节点类型
     * @param <K>            键类型
     * @return 根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> List<T> build(Collection<T> nodes,
                                       Function<T, K> idGetter,
                                       Function<T, K> parentGetter,
                                       Function<T, List<T>> childrenGetter,
                                       BiConsumer<T, List<T>> childrenSetter) {
        return builder(idGetter, parentGetter, childrenGetter, childrenSetter).build(nodes);
    }

    /**
     * 快捷建树并同级排序（递归全深度）。
     *
     * @param nodes          扁平列表
     * @param idGetter       主键
     * @param parentGetter   父编码
     * @param childrenGetter 子读
     * @param childrenSetter 子写
     * @param comparator     同级比较器
     * @param <T>            节点类型
     * @param <K>            键类型
     * @return 根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> List<T> build(Collection<T> nodes,
                                       Function<T, K> idGetter,
                                       Function<T, K> parentGetter,
                                       Function<T, List<T>> childrenGetter,
                                       BiConsumer<T, List<T>> childrenSetter,
                                       Comparator<T> comparator) {
        return builder(idGetter, parentGetter, childrenGetter, childrenSetter)
                .sortBy(comparator)
                .build(nodes);
    }

    /**
     * 按 Integer 排序字段升序建树（null 排最后）。
     *
     * @param nodes          扁平列表
     * @param idGetter       主键
     * @param parentGetter   父编码
     * @param childrenGetter 子读
     * @param childrenSetter 子写
     * @param sortGetter     排序字段
     * @param <T>            节点类型
     * @param <K>            键类型
     * @return 根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> List<T> buildSortedBy(Collection<T> nodes,
                                               Function<T, K> idGetter,
                                               Function<T, K> parentGetter,
                                               Function<T, List<T>> childrenGetter,
                                               BiConsumer<T, List<T>> childrenSetter,
                                               Function<T, Integer> sortGetter) {
        Comparator<T> comparator = Comparator.comparing(
                sortGetter,
                Comparator.nullsLast(Integer::compareTo));
        return build(nodes, idGetter, parentGetter, childrenGetter, childrenSetter, comparator);
    }

    /**
     * 树前序展平（深度优先）。
     *
     * @param roots          根列表
     * @param childrenGetter 子节点读
     * @param <T>            节点类型
     * @return 扁平列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T> List<T> flatten(Collection<T> roots, Function<T, List<T>> childrenGetter) {
        List<T> flat = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return flat;
        }
        Objects.requireNonNull(childrenGetter, "childrenGetter");
        for (T root : roots) {
            flattenVisit(root, childrenGetter, flat);
        }
        return flat;
    }

    private static <T> void flattenVisit(T node, Function<T, List<T>> childrenGetter, List<T> out) {
        if (node == null) {
            return;
        }
        out.add(node);
        List<T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (T child : children) {
            flattenVisit(child, childrenGetter, out);
        }
    }
}
