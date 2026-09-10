package cn.miyf.auth.security;

import org.springframework.stereotype.Component;

/**
 * 平台超管角色定义（公共模块，全局扫描重建）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Component
@PopedomRole(
        code = PlatformRoles.SUPER_ADMIN,
        name = "超级管理员",
        type = PopedomRoleType.SUPER,
        description = "全局超管：绑定全部权限组"
)
public class PlatformRoleDefinitions {
}
