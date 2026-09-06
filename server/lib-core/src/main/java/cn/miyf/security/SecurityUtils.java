package cn.miyf.security;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 安全上下文工具：读取当前登录主体。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前可选主体。
     * <p>
     * 优先从 {@link LoginUserContext}（LocalThreadMap）读取，其次回落 SecurityContext。
     *
     * @return Optional 主体
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static Optional<AuthPrincipal> currentPrincipal() {
        AuthPrincipal fromThread = LoginUserContext.get();
        if (fromThread != null) {
            return Optional.of(fromThread);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * 要求已登录，否则抛未认证异常。
     *
     * @return 当前主体
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static AuthPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 要求当前为普通用户。
     *
     * @return 用户主体
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static AuthPrincipal requireUser() {
        AuthPrincipal principal = requirePrincipal();
        if (principal.getType() != PrincipalType.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要用户登录");
        }
        return principal;
    }

    /**
     * 要求当前为管理员。
     *
     * @return 管理员主体
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static AuthPrincipal requireAdmin() {
        AuthPrincipal principal = requirePrincipal();
        if (principal.getType() != PrincipalType.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员登录");
        }
        return principal;
    }

    /**
     * 当前用户 ID（仅 USER）。
     *
     * @return 用户 ID
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static Long currentUserId() {
        return requireUser().getId();
    }

    /**
     * 当前管理员 ID（仅 ADMIN）。
     *
     * @return 管理员 ID
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public static Long currentAdminId() {
        return requireAdmin().getId();
    }
}
