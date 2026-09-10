package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 厨房个人端权限组（{@link KitchenPopedomCodes.Personal}），展示名「个人-厨房服务」。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = KitchenPopedomCodes.Personal.CODE,
        scope = PopedomScope.PERSONAL,
        service = KitchenPopedomCodes.Personal.SERVICE,
        product = KitchenPopedomCodes.Personal.PRODUCT,
        roles = {KitchenRoleCodes.DEFAULT},
        sort = KitchenPopedomCodes.Personal.SORT
)
public @interface KitchenPersonalPopedom {
}
