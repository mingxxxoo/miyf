package cn.miyf.gateway.security;

import cn.miyf.common.ApiResult;
import cn.miyf.common.ErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 异常 → 统一 {@link ApiResult}（放在 gateway，避免 core-common 依赖 security）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    /**
     * 处理认证失败。
     *
     * @param ex 认证异常
     * @return 未登录响应
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuth(Exception ex) {
        return ApiResult.fail(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 处理无权限访问（含方法鉴权拒绝）。
     *
     * @param ex 访问拒绝异常
     * @return 无权限响应
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDenied(Exception ex) {
        return ApiResult.fail(ErrorCode.FORBIDDEN);
    }
}
