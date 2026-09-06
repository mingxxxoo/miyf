package cn.miyf.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应包装。
 * <p>
 * 成功：code = 0；失败：业务错误码 + message，data 可为 null。
 *
 * @param <T> 业务数据类型
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(int code, String message, T data) {

    /**
     * 成功响应（带数据）。
     *
     * @param data 业务数据
     * @param <T>  类型
     * @return 成功结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data);
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 类型
     * @return 成功结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    /**
     * 失败响应。
     *
     * @param code    错误码
     * @param message 错误文案
     * @param <T>     类型
     * @return 失败结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    /**
     * 按枚举错误码构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       类型
     * @return 失败结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
