package cn.miyf.infrastructure.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由：基于 ThreadLocal 选择 PRIMARY / SECONDARY。
 * <p>
 * 当前版本业务默认走 PRIMARY；跨库事务不在本版本范围。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public class DataSourceRouter extends AbstractRoutingDataSource {

    private static final ThreadLocal<DataSourceKey> CONTEXT = new ThreadLocal<>();

    /**
     * 绑定当前线程使用的数据源键。
     *
     * @param key 数据源键
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public static void use(DataSourceKey key) {
        CONTEXT.set(key);
    }

    /**
     * 清理线程上下文，防止线程复用泄漏。
     *
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 决定当前查找键；未设置时回落 PRIMARY。
     *
     * @return 路由键
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceKey key = CONTEXT.get();
        return key == null ? DataSourceKey.PRIMARY : key;
    }
}
