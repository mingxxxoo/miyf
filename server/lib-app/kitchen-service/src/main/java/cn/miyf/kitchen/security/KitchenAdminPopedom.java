package cn.miyf.kitchen.security;

import cn.miyf.security.PopedomGroup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 厨房管理端权限组（{@link KitchenPopedomCodes.Admin}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = KitchenPopedomCodes.Admin.CODE,
        name = KitchenPopedomCodes.Admin.NAME,
        product = KitchenPopedomCodes.Admin.PRODUCT,
        sort = KitchenPopedomCodes.Admin.SORT
)
public @interface KitchenAdminPopedom {
}
