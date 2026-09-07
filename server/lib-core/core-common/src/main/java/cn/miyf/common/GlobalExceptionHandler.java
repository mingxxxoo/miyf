package cn.miyf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转换为 {@link ApiResult}，避免向前端泄露堆栈与内部细节。
 * <p>
 * Spring Security 相关异常由 gateway 模块的 {@code SecurityExceptionHandler} 处理，
 * 避免 core-common 依赖 spring-security。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理可预期的业务异常；未登录/无权限时返回对应 HTTP 状态（401/403）。
     *
     * @param ex 业务异常
     * @return 统一失败响应
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Object>> handleBusiness(BusinessException ex) {
        HttpStatus status = HttpStatus.OK;
        if (ex.getCode() == ErrorCode.UNAUTHORIZED.getCode()) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.getCode() == ErrorCode.FORBIDDEN.getCode()) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(ApiResult.fail(ex.getCode(), ex.getMessage(), ex.getData()));
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
