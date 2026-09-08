package cn.miyf.infrastructure.cache;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 列表缓存：适合分类列表、热门榜、下拉选项等扁平集合。
 *
 * @param <E> 元素类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface ListCache<E> extends ValueCache<List<E>> {

    /**
     * 读取或加载列表（语义别名，便于业务表达）。
     *
     * @param key    缓存键
     * @param ttl    正常 TTL
     * @param loader 列表加载器
     * @return 列表（可为 null，取决于 loader）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default List<E> getOrLoadList(String key, Duration ttl, Supplier<List<E>> loader) {
        return getOrLoad(key, ttl, loader);
    }
}
