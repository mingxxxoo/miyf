package cn.miyf.infrastructure.datasource;

/**
 * 数据库厂商类型枚举。
 * <p>
 * 当前默认 POSTGRESQL；切换厂商应主要改 Infrastructure，避免侵入 Domain/Service。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public enum DatabaseType {
    POSTGRESQL,
    MYSQL,
    MARIADB,
    ORACLE,
    SQLSERVER;

    /**
     * 解析配置字符串，空白时回落 PostgreSQL。
     *
     * @param value 配置值
     * @return 数据库类型
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public static DatabaseType from(String value) {
        if (value == null || value.isBlank()) {
            return POSTGRESQL;
        }
        return DatabaseType.valueOf(value.trim().toUpperCase());
    }
}
