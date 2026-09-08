package cn.miyf.health.security;

import cn.miyf.auth.security.PopedomGroup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 健康个人端权限组（{@link HealthPopedomCodes.Personal}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = HealthPopedomCodes.Personal.CODE,
        name = HealthPopedomCodes.Personal.NAME,
        product = HealthPopedomCodes.Personal.PRODUCT,
        sort = HealthPopedomCodes.Personal.SORT
)
public @interface HealthPersonalPopedom {
}
