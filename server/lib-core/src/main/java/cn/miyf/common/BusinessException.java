package cn.miyf.common;

/**
 * 可预期业务异常，由 {@link GlobalExceptionHandler} 转换为统一响应。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 使用错误码枚举构造。
     *
     * @param errorCode 错误码
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码枚举与自定义文案构造。
     *
     * @param errorCode 错误码
     * @param message   自定义文案
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用原始错误码与文案构造。
     *
     * @param code    错误码
     * @param message 文案
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
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
}
