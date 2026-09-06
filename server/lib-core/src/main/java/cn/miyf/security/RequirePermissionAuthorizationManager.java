package cn.miyf.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Spring Security 方法鉴权：校验 {@link RequirePermission} 声明的权限码（满足其一即可）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Component
public class RequirePermissionAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    /**
     * 按注解权限码决策是否放行。
     *
     * @param authentication 认证信息
     * @param invocation     方法调用
     * @return 授权结果
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         MethodInvocation invocation) {
        RequirePermission annotation = AnnotationUtils.findAnnotation(invocation.getMethod(), RequirePermission.class);
        if (annotation == null && invocation.getThis() != null) {
            annotation = AnnotationUtils.findAnnotation(invocation.getThis().getClass(), RequirePermission.class);
        }
        if (annotation == null || annotation.value().length == 0) {
            return new AuthorizationDecision(true);
        }

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("未登录");
        }
        if (principal.getType() != PrincipalType.ADMIN) {
            return new AuthorizationDecision(false);
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        boolean granted = Arrays.stream(annotation.value()).anyMatch(authorities::contains);
        return new AuthorizationDecision(granted);
    }
}
