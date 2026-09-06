package cn.miyf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转换为 {@link ApiResult}，避免向前端泄露堆栈与内部细节。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理可预期的业务异常。
     *
     * @param ex 业务异常
     * @return 统一失败响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleBusiness(BusinessException ex) {
        return ApiResult.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验失败。
     *
     * @param ex 校验或非法参数异常
     * @return 统一失败响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleValidation(Exception ex) {
        String message = ErrorCode.BAD_REQUEST.getMessage();
        // 优先取字段级校验文案，便于前端直接展示
        if (ex instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
            message = manv.getBindingResult().getFieldError().getDefaultMessage();
        } else if (ex instanceof BindException be && be.getBindingResult().getFieldError() != null) {
            message = be.getBindingResult().getFieldError().getDefaultMessage();
        } else if (ex.getMessage() != null) {
            message = ex.getMessage();
        }
        return ApiResult.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理认证失败。
     *
     * @param ex 认证异常
     * @return 未登录响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleAuth(Exception ex) {
        return ApiResult.fail(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 处理无权限访问（含 Spring Security 方法鉴权拒绝）。
     *
     * @param ex 访问拒绝异常
     * @return 无权限响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleAccessDenied(Exception ex) {
        return ApiResult.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * 兜底未知异常：仅记录日志，不返回堆栈。
     *
     * @param ex 未知异常
     * @return 系统繁忙响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResult.fail(ErrorCode.INTERNAL_ERROR);
    }
}
