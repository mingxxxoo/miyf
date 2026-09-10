package cn.miyf.health.security;

import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 健康单位端权限组，展示名「单位-健康服务」。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = HealthPopedomCodes.Org.CODE,
        scope = PopedomScope.ORG,
        service = HealthPopedomCodes.Org.SERVICE,
        product = HealthPopedomCodes.Org.PRODUCT,
        roles = {HealthRoleCodes.ORG},
        sort = HealthPopedomCodes.Org.SORT
)
public @interface HealthOrgPopedom {
}
