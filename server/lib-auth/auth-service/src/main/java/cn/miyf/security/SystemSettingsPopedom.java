package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统设置权限组（{@link PopedomCodes.SystemSettings}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = PopedomCodes.SystemSettings.CODE,
        name = PopedomCodes.SystemSettings.NAME,
        product = PopedomCodes.SystemSettings.PRODUCT,
        sort = PopedomCodes.SystemSettings.SORT
)
public @interface SystemSettingsPopedom {
}
