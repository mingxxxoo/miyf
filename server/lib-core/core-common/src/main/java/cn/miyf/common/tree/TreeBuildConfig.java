package cn.miyf.common.tree;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 建树配置：根判定、同级排序、孤儿节点策略。
 *
 * @param <T> 节点类型
 * @param <K> 主键 / 父编码类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class TreeBuildConfig<T, K> {

    private final Predicate<K> rootParentPredicate;
    private final Comparator<T> comparator;
    private final boolean orphanAsRoot;

    private TreeBuildConfig(Predicate<K> rootParentPredicate,
                            Comparator<T> comparator,
                            boolean orphanAsRoot) {
        this.rootParentPredicate = rootParentPredicate;
        this.comparator = comparator;
        this.orphanAsRoot = orphanAsRoot;
    }

    /**
     * 默认配置：父编码为 null 视为根；孤儿挂为根；不排序。
     *
     * @param <T> 节点类型
     * @param <K> 键类型
     * @return 配置
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> TreeBuildConfig<T, K> defaults() {
        return new TreeBuildConfig<>(Objects::isNull, null, true);
    }

    /**
     * 复制并设置根父编码判定。
     *
     * @param rootParentPredicate 父编码满足时视为根（如 null、0、""）
     * @return 新配置
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuildConfig<T, K> rootWhen(Predicate<K> rootParentPredicate) {
        return new TreeBuildConfig<>(
                Objects.requireNonNull(rootParentPredicate, "rootParentPredicate"),
                comparator,
                orphanAsRoot);
    }

    /**
     * 将若干父编码值视为根（与 {@link #rootWhen} 等价的便捷写法）。
     *
     * @param rootParentValues 根父编码集合，如 null、0L、"0"、""
     * @return 新配置
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @SafeVarargs
    public final TreeBuildConfig<T, K> rootParentValues(K... rootParentValues) {
        // HashSet 允许显式包含 null（Set.of 不允许）
        Set<K> roots = new HashSet<>();
        if (rootParentValues != null) {
            for (K value : rootParentValues) {
                roots.add(value);
            }
        }
        return rootWhen(roots::contains);
    }

    /**
     * 同级排序比较器；建树后对各层 children 递归排序。
     *
     * @param comparator 比较器，null 表示保持输入相对顺序
     * @return 新配置
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuildConfig<T, K> sortBy(Comparator<T> comparator) {
        return new TreeBuildConfig<>(rootParentPredicate, comparator, orphanAsRoot);
    }

    /**
     * 父节点不在本次列表中时，是否作为根返回（默认 true）。
     * false 时丢弃孤儿节点。
     *
     * @param orphanAsRoot 是否把孤儿当根
     * @return 新配置
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuildConfig<T, K> orphanAsRoot(boolean orphanAsRoot) {
        return new TreeBuildConfig<>(rootParentPredicate, comparator, orphanAsRoot);
    }

    /**
     * @return 根父编码判定
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Predicate<K> rootParentPredicate() {
        return rootParentPredicate;
    }

    /**
     * @return 同级排序，可为 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Comparator<T> comparator() {
        return comparator;
    }

    /**
     * @return 孤儿是否当根
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean orphanAsRoot() {
        return orphanAsRoot;
    }
}
