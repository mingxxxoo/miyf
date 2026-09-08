package cn.miyf.common.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 无限极树构建器：由扁平列表按「主键 ↔ 父编码」挂载 children，深度不限。
 *
 * <pre>{@code
 * List&lt;MenuVo&gt; roots = TreeBuilder.of(MenuVo::getId, MenuVo::getParentId,
 *         MenuVo::getChildren, MenuVo::setChildren)
 *     .rootWhen(pid -&gt; pid == null || pid == 0L)
 *     .sortBy(Comparator.comparing(MenuVo::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
 *     .build(flatList);
 * }</pre>
 *
 * @param <T> 节点类型
 * @param <K> 主键 / 父编码类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class TreeBuilder<T, K> {

    private final TreeNodeOps<T, K> ops;
    private TreeBuildConfig<T, K> config;

    private TreeBuilder(TreeNodeOps<T, K> ops, TreeBuildConfig<T, K> config) {
        this.ops = ops;
        this.config = config;
    }

    /**
     * 基于字段访问器创建构建器。
     *
     * @param idGetter       主键
     * @param parentGetter   父编码（可定制字段）
     * @param childrenGetter 子列表读
     * @param childrenSetter 子列表写
     * @param <T>            节点类型
     * @param <K>            键类型
     * @return 构建器
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> TreeBuilder<T, K> of(Function<T, K> idGetter,
                                              Function<T, K> parentGetter,
                                              Function<T, List<T>> childrenGetter,
                                              BiConsumer<T, List<T>> childrenSetter) {
        return new TreeBuilder<>(
                new TreeNodeOps<>(idGetter, parentGetter, childrenGetter, childrenSetter),
                TreeBuildConfig.defaults());
    }

    /**
     * 基于已封装的节点操作创建构建器。
     *
     * @param ops 节点操作
     * @param <T> 节点类型
     * @param <K> 键类型
     * @return 构建器
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T, K> TreeBuilder<T, K> of(TreeNodeOps<T, K> ops) {
        return new TreeBuilder<>(Objects.requireNonNull(ops, "ops"), TreeBuildConfig.defaults());
    }

    /**
     * 自定义完整配置。
     *
     * @param config 配置
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuilder<T, K> config(TreeBuildConfig<T, K> config) {
        this.config = Objects.requireNonNull(config, "config");
        return this;
    }

    /**
     * 父编码满足判定时视为根节点。
     *
     * @param rootParentPredicate 根判定
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuilder<T, K> rootWhen(Predicate<K> rootParentPredicate) {
        this.config = config.rootWhen(rootParentPredicate);
        return this;
    }

    /**
     * 指定若干父编码值视为根。
     *
     * @param rootParentValues 根父编码
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @SafeVarargs
    public final TreeBuilder<T, K> rootParentValues(K... rootParentValues) {
        this.config = config.rootParentValues(rootParentValues);
        return this;
    }

    /**
     * 同级排序（递归作用于每一层 children，支持无限深度）。
     *
     * @param comparator 比较器
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuilder<T, K> sortBy(Comparator<T> comparator) {
        this.config = config.sortBy(comparator);
        return this;
    }

    /**
     * 父不在列表中时是否挂为根。
     *
     * @param orphanAsRoot true=当根；false=丢弃
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public TreeBuilder<T, K> orphanAsRoot(boolean orphanAsRoot) {
        this.config = config.orphanAsRoot(orphanAsRoot);
        return this;
    }

    /**
     * 将扁平列表构建为无限极树，返回根节点列表。
     *
     * @param nodes 扁平节点（同一对象会被挂到树中，children 会被清空后重填）
     * @return 根列表；空输入返回空列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<T> build(Collection<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // LinkedHashMap：根顺序尽量贴近输入顺序
        Map<K, T> index = new LinkedHashMap<>(nodes.size() * 2);
        for (T node : nodes) {
            if (node == null) {
                continue;
            }
            K id = ops.idGetter().apply(node);
            if (id == null) {
                throw new IllegalArgumentException("树节点主键不能为空");
            }
            // 先清空旧 children，避免重复挂载
            ensureMutableChildren(node).clear();
            T previous = index.put(id, node);
            if (previous != null) {
                throw new IllegalArgumentException("树节点主键重复: " + id);
            }
        }

        List<T> roots = new ArrayList<>();
        Predicate<K> isRootParent = config.rootParentPredicate();
        for (T node : index.values()) {
            K parentKey = ops.parentGetter().apply(node);
            if (isRootParent.test(parentKey)) {
                roots.add(node);
                continue;
            }
            T parent = index.get(parentKey);
            if (parent != null) {
                // 父在列表中则挂载；深度由父子链自然延伸，无层数上限
                ensureMutableChildren(parent).add(node);
            } else if (config.orphanAsRoot()) {
                roots.add(node);
            }
        }

        Comparator<T> comparator = config.comparator();
        if (comparator != null) {
            sortRecursively(roots, comparator);
        }
        return roots;
    }

    /**
     * 先映射再建树（Entity → VO），仍支持无限极。
     *
     * @param sources 源列表
     * @param mapper  映射函数
     * @param <S>     源类型
     * @return 根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public <S> List<T> buildMapped(Collection<S> sources, Function<? super S, ? extends T> mapper) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        Objects.requireNonNull(mapper, "mapper");
        List<T> mapped = new ArrayList<>(sources.size());
        for (S source : sources) {
            if (source == null) {
                continue;
            }
            T node = mapper.apply(source);
            if (node != null) {
                mapped.add(node);
            }
        }
        return build(mapped);
    }

    /**
     * 将树前序展平为列表（深度优先），不修改原树。
     *
     * @param roots 根列表
     * @return 扁平列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<T> flatten(Collection<T> roots) {
        List<T> flat = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return flat;
        }
        for (T root : roots) {
            flattenVisit(root, flat);
        }
        return flat;
    }

    private void flattenVisit(T node, List<T> out) {
        if (node == null) {
            return;
        }
        out.add(node);
        List<T> children = ops.childrenGetter().apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (T child : children) {
            flattenVisit(child, out);
        }
    }

    private void sortRecursively(List<T> nodes, Comparator<T> comparator) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(comparator);
        for (T node : nodes) {
            List<T> children = ops.childrenGetter().apply(node);
            if (children == null || children.isEmpty()) {
                continue;
            }
            sortRecursively(ensureMutableChildren(node), comparator);
        }
    }

    private List<T> ensureMutableChildren(T node) {
        List<T> children = ops.childrenGetter().apply(node);
        if (children == null) {
            children = new ArrayList<>();
            ops.childrenSetter().accept(node, children);
            return children;
        }
        if (!(children instanceof ArrayList)) {
            children = new ArrayList<>(children);
            ops.childrenSetter().accept(node, children);
        }
        return children;
    }
}
