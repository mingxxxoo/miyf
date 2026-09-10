package cn.miyf.auth.security;

import java.util.List;

/**
 * 管理员登录时加载角色码与权限码（由 permission-service 实现，避免 auth↔permission 循环依赖）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public interface AdminAuthAuthorityLoader {

    /**
     * 用户角色码列表。
     *
     * @param userId 用户 ID
     * @return 角色码
     */
    List<String> loadRoleCodes(Long userId);

    /**
     * 用户权限码列表。
     *
     * @param userId 用户 ID
     * @return 权限码
     */
    List<String> loadPermissionCodes(Long userId);

    /**
     * 用户有效数据范围（多角色取最宽）。
     *
     * @param userId 用户 ID
     * @return 数据范围枚举名
     */
    default String loadEffectiveDataScope(Long userId) {
        return DataScope.ALL.name();
    }

    /**
     * 若用户尚无任何角色，则绑定各产品域个人默认角色（{@code is_default=true}）。
     * <p>
     * 与创建用户未指定角色、以及权限启动重建后的默认授予对齐；已有角色时不改动。
     *
     * @param userId 用户 ID
     * @return 是否发生了绑定
     */
    default boolean ensureDefaultRolesIfAbsent(Long userId) {
        return false;
    }
}
