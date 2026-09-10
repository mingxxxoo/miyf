package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 厨房单位端权限组（{@link KitchenPopedomCodes.Org}），展示名「单位-厨房服务」。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = KitchenPopedomCodes.Org.CODE,
        scope = PopedomScope.ORG,
        service = KitchenPopedomCodes.Org.SERVICE,
        product = KitchenPopedomCodes.Org.PRODUCT,
        roles = {KitchenRoleCodes.ORG},
        sort = KitchenPopedomCodes.Org.SORT
)
public @interface KitchenOrgPopedom {
}
