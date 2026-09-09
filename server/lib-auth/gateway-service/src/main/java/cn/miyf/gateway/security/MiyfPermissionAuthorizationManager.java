package cn.miyf.gateway.security;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.RequirePermission;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
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
 * <ul>
 *   <li>匿名：放行（由 HttpSecurity 控制公开路径）</li>
 *   <li>管理员：必须持有对应权限码</li>
 *   <li>普通用户：仅允许访问「个人」权限组接口；JWT 未挂 API 权限码时登录即可，已挂码则按码校验</li>
 * </ul>
 * 注意：{@link AuthPrincipal#getAuthorities()} 始终含 {@code ROLE_USER}/{@code ROLE_ADMIN}，
 * 判断「是否已挂权限码」必须看 {@link AuthPrincipal#getPermissions()}，不能用 authorities 是否为空。
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
            // 匿名交给 HttpSecurity（如 /dishes/** permitAll）
            return new AuthorizationDecision(true);
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (principal.getType() == PrincipalType.ADMIN) {
            return new AuthorizationDecision(authorities.contains(annotation.code()));
        }

        if (principal.getType() == PrincipalType.USER) {
            PopedomGroup popedom = resolvePopedomGroup(invocation);
            if (!isPersonalPopedom(popedom)) {
                return new AuthorizationDecision(false);
            }
            // 未挂 API 权限码（兼容旧 token / 默认角色尚未就绪）：个人接口登录即可
            if (principal.getPermissions() == null || principal.getPermissions().isEmpty()) {
                return new AuthorizationDecision(true);
            }
            return new AuthorizationDecision(authorities.contains(annotation.code()));
        }

        return new AuthorizationDecision(false);
    }

    private static PopedomGroup resolvePopedomGroup(MethodInvocation invocation) {
        Class<?> targetClass = invocation.getThis() == null ? null : invocation.getThis().getClass();
        if (targetClass != null) {
            PopedomGroup onClass = AnnotationUtils.findAnnotation(targetClass, PopedomGroup.class);
            if (onClass != null) {
                return onClass;
            }
        }
        return AnnotationUtils.findAnnotation(invocation.getMethod().getDeclaringClass(), PopedomGroup.class);
    }

    private static boolean isPersonalPopedom(PopedomGroup popedom) {
        return popedom != null && "个人".equals(popedom.name());
    }
}
