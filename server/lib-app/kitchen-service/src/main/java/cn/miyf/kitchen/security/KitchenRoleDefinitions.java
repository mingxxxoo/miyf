package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PopedomRole;
import cn.miyf.auth.security.PopedomRoleType;
import cn.miyf.auth.security.PopedomRoles;
import org.springframework.stereotype.Component;

/**
 * 厨房模块角色定义（启动扫描重建）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Component
@PopedomRoles({
        @PopedomRole(
                code = KitchenRoleCodes.DEFAULT,
                name = "厨房默认角色",
                type = PopedomRoleType.DEFAULT,
                product = KitchenPopedomCodes.Personal.PRODUCT,
                description = "厨房个人端默认角色，业务上所有厨房用户可拥有"
        ),
        @PopedomRole(
                code = KitchenRoleCodes.ORG,
                name = "厨房单位角色",
                type = PopedomRoleType.ORG,
                product = KitchenPopedomCodes.Org.PRODUCT,
                description = "厨房单位端角色"
        )
})
public class KitchenRoleDefinitions {
}
