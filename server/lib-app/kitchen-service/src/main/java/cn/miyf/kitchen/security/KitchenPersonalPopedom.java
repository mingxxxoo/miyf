package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PopedomGroup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 厨房个人端（普通用户 / 小程序）权限组（{@link KitchenPopedomCodes.Personal}）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = KitchenPopedomCodes.Personal.CODE,
        name = KitchenPopedomCodes.Personal.NAME,
        product = KitchenPopedomCodes.Personal.PRODUCT,
        sort = KitchenPopedomCodes.Personal.SORT
)
public @interface KitchenPersonalPopedom {
}
