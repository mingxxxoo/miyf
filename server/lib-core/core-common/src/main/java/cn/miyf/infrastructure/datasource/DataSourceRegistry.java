package cn.miyf.infrastructure.datasource;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源注册表：保存已创建的 Druid 实例，供路由与运维查询。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Component
public class DataSourceRegistry {

    private final Map<DataSourceKey, DataSource> sources = new EnumMap<>(DataSourceKey.class);

    /**
     * 注册数据源。
     *
     * @param key        路由键
     * @param dataSource 数据源实例
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public void register(DataSourceKey key, DataSource dataSource) {
        sources.put(key, dataSource);
    }

    /**
     * 按键获取数据源。
     *
     * @param key 路由键
     * @return 可选数据源
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public Optional<DataSource> get(DataSourceKey key) {
        return Optional.ofNullable(sources.get(key));
    }

    /**
     * 返回全部已注册数据源（只读视图）。
     *
     * @return 键到数据源映射
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public Map<DataSourceKey, DataSource> all() {
        return Map.copyOf(sources);
    }
}
