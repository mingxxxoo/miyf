package cn.miyf.health.security;

import cn.miyf.auth.security.PlatformRoles;
import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 健康超管端权限组，展示名「超管-健康服务」。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = HealthPopedomCodes.Admin.CODE,
        scope = PopedomScope.SUPER,
        service = HealthPopedomCodes.Admin.SERVICE,
        product = HealthPopedomCodes.Admin.PRODUCT,
        roles = {PlatformRoles.SUPER_ADMIN},
        sort = HealthPopedomCodes.Admin.SORT
)
public @interface HealthAdminPopedom {
}
