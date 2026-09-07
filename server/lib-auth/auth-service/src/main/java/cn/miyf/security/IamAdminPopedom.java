package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IAM 管理员权限组（{@link PopedomCodes.IamAdmin}）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PopedomGroup(
        value = PopedomCodes.IamAdmin.CODE,
        name = PopedomCodes.IamAdmin.NAME,
        product = PopedomCodes.IamAdmin.PRODUCT,
        sort = PopedomCodes.IamAdmin.SORT
)
public @interface IamAdminPopedom {
}
