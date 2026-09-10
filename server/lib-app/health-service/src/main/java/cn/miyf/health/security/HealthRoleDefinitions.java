package cn.miyf.health.security;

import cn.miyf.auth.security.PopedomRole;
import cn.miyf.auth.security.PopedomRoleType;
import cn.miyf.auth.security.PopedomRoles;
import org.springframework.stereotype.Component;

/**
 * 健康模块角色定义（启动扫描重建）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Component
@PopedomRoles({
        @PopedomRole(
                code = HealthRoleCodes.DEFAULT,
                name = "健康默认角色",
                type = PopedomRoleType.DEFAULT,
                product = HealthPopedomCodes.Personal.PRODUCT,
                description = "健康个人端默认角色"
        ),
        @PopedomRole(
                code = HealthRoleCodes.ORG,
                name = "健康单位角色",
                type = PopedomRoleType.ORG,
                product = HealthPopedomCodes.Org.PRODUCT,
                description = "健康单位端角色"
        )
})
public class HealthRoleDefinitions {
}
