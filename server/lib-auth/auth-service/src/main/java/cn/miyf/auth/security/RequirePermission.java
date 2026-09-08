package cn.miyf.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级多权限 OR 校验：主体需具备 {@link #value()} 中<strong>任一</strong>权限码，且须为管理员。
 * <p>
 * 单权限码场景请只用 {@link MiyfPermission}（已自带同等鉴权），不必再叠本注解。
 * 本注解适用于「一个接口允许多个权限之一」的场景，例如详情接口允许 {@code list} 或 {@code detail}。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限码列表，满足其一即可。
     *
     * @return 权限码
     */
    String[] value();
}
