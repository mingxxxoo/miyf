package cn.miyf.infrastructure.datasource;

/**
 * 数据源路由键。
 * <p>
 * PRIMARY 为当前业务主库；SECONDARY 为预留扩展，默认可不启用。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public enum DataSourceKey {
    PRIMARY,
    SECONDARY
}
