package cn.miyf.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统设置超管权限组（{@link PopedomCodes.SystemSettings}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = PopedomCodes.SystemSettings.CODE,
        scope = PopedomScope.SUPER,
        service = PopedomCodes.SystemSettings.SERVICE,
        product = PopedomCodes.SystemSettings.PRODUCT,
        roles = {PlatformRoles.SUPER_ADMIN},
        sort = PopedomCodes.SystemSettings.SORT
)
public @interface SystemSettingsPopedom {
}
