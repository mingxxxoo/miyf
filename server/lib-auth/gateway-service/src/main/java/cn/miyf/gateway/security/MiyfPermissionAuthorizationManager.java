package cn.miyf.gateway.security;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.RequirePermission;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Spring Security 方法鉴权：校验 {@link MiyfPermission#code()}。
 * <p>
 * 若同方法已标注 {@link RequirePermission}，则交由后者处理，避免重复拒绝。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Component
public class MiyfPermissionAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    /**
     * 按 MiyfPermission 权限码决策。
     *
     * @param authentication 认证信息
     * @param invocation     方法调用
     * @return 授权结果
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         MethodInvocation invocation) {
        if (AnnotationUtils.findAnnotation(invocation.getMethod(), RequirePermission.class) != null) {
            return new AuthorizationDecision(true);
        }
        MiyfPermission annotation = AnnotationUtils.findAnnotation(invocation.getMethod(), MiyfPermission.class);
        if (annotation == null || annotation.code() == null || annotation.code().isBlank()) {
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
        return new AuthorizationDecision(authorities.contains(annotation.code()));
    }
}
