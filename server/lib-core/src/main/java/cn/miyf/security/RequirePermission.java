package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限校验：由 Spring Security {@code AuthorizationManager} 校验
 * {@link AuthPrincipal#getAuthorities()} 中是否具备指定权限码之一。
 * <p>
 * 等价于 {@code @PreAuthorize("hasAnyAuthority('a','b')")}，并额外要求主体为管理员。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限码列表，满足其一即可（写入 JWT / SecurityContext 的 Authority）。
     *
     * @return 权限码
     */
    String[] value();
}
