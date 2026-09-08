package cn.miyf.common;

import lombok.Getter;

/**
 * 业务错误码定义。
 * <p>
 * 0 表示成功；4xxxx 为业务/客户端错误；5xxxx 为系统错误。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "没有权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    STOCK_INSUFFICIENT(41001, "可提供份数不足"),
    INVALID_STATUS(41002, "状态不允许此操作"),
    DUPLICATE_COMMENT(41003, "该预约菜品已评价过"),
    ORDER_NOT_COMMENTABLE(41004, "当前预约不可评价"),
    USER_DISABLED(41005, "用户已被禁用"),
    LOGIN_FAILED(41006, "用户名或密码错误"),
    WX_AUTH_FAILED(41007, "微信登录失败"),
    INVALID_FILE(41008, "文件类型或内容不合法"),
    FILE_TOO_LARGE(41009, "文件过大"),
    STORAGE_UNAVAILABLE(41010, "文件存储暂不可用"),
    CAPTCHA_REQUIRED(41011, "请输入验证码"),
    ACCOUNT_LOCKED(41012, "登录失败次数过多，账号已临时锁定"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后再试"),
    LOCK_BUSY(42901, "系统繁忙，请稍后重试"),
    INTERNAL_ERROR(50000, "系统繁忙，请稍后再试");

    /**
     * -- GETTER --
     *  获取数字错误码。
     *
     * @return 错误码
     *
     */
    private final int code;
    /**
     * -- GETTER --
     *  获取默认错误文案。
     *
     * @return 文案
     *
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
