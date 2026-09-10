package cn.miyf.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IAM 超管权限组（{@link PopedomCodes.IamAdmin}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = PopedomCodes.IamAdmin.CODE,
        scope = PopedomScope.SUPER,
        service = PopedomCodes.IamAdmin.SERVICE,
        product = PopedomCodes.IamAdmin.PRODUCT,
        roles = {PlatformRoles.SUPER_ADMIN},
        sort = PopedomCodes.IamAdmin.SORT
)
public @interface IamAdminPopedom {
}
