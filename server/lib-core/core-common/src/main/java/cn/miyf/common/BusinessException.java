package cn.miyf.common;

/**
 * 可预期业务异常，由 {@link GlobalExceptionHandler} 转换为统一响应。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final Object data;

    /**
     * 使用错误码枚举构造。
     *
     * @param errorCode 错误码
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 使用错误码枚举与自定义文案构造。
     *
     * @param errorCode 错误码
     * @param message   自定义文案
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, null);
    }

    /**
     * 使用原始错误码与文案构造。
     *
     * @param code    错误码
     * @param message 文案
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    /**
     * 带附加数据的业务异常（如登录风控状态）。
     *
     * @param errorCode 错误码
     * @param message   文案
     * @param data      附加数据
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        this(errorCode.getCode(), message, data);
    }

    /**
     * 带附加数据的业务异常。
     *
     * @param code    错误码
     * @param message 文案
     * @param data    附加数据
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * 获取业务错误码。
     *
     * @return 错误码
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取附加数据。
     *
     * @return 数据，可空
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public Object getData() {
        return data;
    }
}
