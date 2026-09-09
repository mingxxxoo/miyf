package cn.miyf.common.tree;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树节点字段访问约定：主键、父编码、子节点列表。
 * 业务 VO / Entity 无需继承接口，通过函数引用适配即可。
 *
 * @param <T> 节点类型
 * @param <K> 主键 / 父编码类型（如 Long、String）
 * @author XieMingJie
 * @since 2026-09-08
 */
@RequiredArgsConstructor
public final class TreeNodeOps<T, K> {

    private final Function<T, K> idGetter;
    private final Function<T, K> parentGetter;
    private final Function<T, List<T>> childrenGetter;
    private final BiConsumer<T, List<T>> childrenSetter;

    
    /**
     * @return 主键读取
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Function<T, K> idGetter() {
        return idGetter;
    }

    /**
     * @return 父编码读取
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Function<T, K> parentGetter() {
        return parentGetter;
    }

    /**
     * @return 子节点读取
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Function<T, List<T>> childrenGetter() {
        return childrenGetter;
    }

    /**
     * @return 子节点写入
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public BiConsumer<T, List<T>> childrenSetter() {
        return childrenSetter;
    }
}
