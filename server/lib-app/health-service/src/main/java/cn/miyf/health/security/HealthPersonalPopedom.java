package cn.miyf.health.security;

import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 健康个人端权限组，展示名「个人-健康服务」。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = HealthPopedomCodes.Personal.CODE,
        scope = PopedomScope.PERSONAL,
        service = HealthPopedomCodes.Personal.SERVICE,
        product = HealthPopedomCodes.Personal.PRODUCT,
        roles = {HealthRoleCodes.DEFAULT},
        sort = HealthPopedomCodes.Personal.SORT
)
public @interface HealthPersonalPopedom {
}
