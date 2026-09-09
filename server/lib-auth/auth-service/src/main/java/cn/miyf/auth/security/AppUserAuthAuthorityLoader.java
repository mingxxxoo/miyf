package cn.miyf.auth.security;

import java.util.List;

/**
 * 用户端（微信 / kitchen_user）权限加载扩展点。
 * <p>
 * 由 permission-service 实现：从各产品域 {@code is_default} 角色（如 {@code default_person}）解析角色码与 API 权限码。
 * 微信用户与管理员分属不同账号表，不能走 {@link AdminAuthAuthorityLoader} 的 sys_user_role 路径。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public interface AppUserAuthAuthorityLoader {

    /**
     * 加载默认角色码（各产品域 is_default=true）。
     *
     * @return 角色码列表，无则空列表
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    List<String> loadDefaultRoleCodes();

    /**
     * 加载默认角色绑定的 API 权限码。
     *
     * @return 权限码列表，无则空列表
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    List<String> loadDefaultPermissionCodes();

    /**
     * 加载默认角色的有效数据范围（多角色取最宽）。
     *
     * @return 数据范围名，如 {@code SELF}
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    String loadDefaultDataScope();
}
