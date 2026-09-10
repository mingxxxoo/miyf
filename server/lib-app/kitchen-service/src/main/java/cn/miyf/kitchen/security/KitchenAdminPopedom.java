package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PlatformRoles;
import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 厨房超管端权限组（{@link KitchenPopedomCodes.Admin}），展示名「超管-厨房服务」。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = KitchenPopedomCodes.Admin.CODE,
        scope = PopedomScope.SUPER,
        service = KitchenPopedomCodes.Admin.SERVICE,
        product = KitchenPopedomCodes.Admin.PRODUCT,
        roles = {PlatformRoles.SUPER_ADMIN},
        sort = KitchenPopedomCodes.Admin.SORT
)
public @interface KitchenAdminPopedom {
}
