package cn.miyf.auth.security;

/**
 * 平台级角色编码（公共模块，全局使用）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
public final class PlatformRoles {

    private PlatformRoles() {
    }

    /** 超级管理员：拥有全部权限组与全部业务角色语义。 */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
}
