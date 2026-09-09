package cn.miyf.oss.enums;

/**
 * 文件访问权限。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public enum FileAccessPermission {

    /** 公开可读。 */
    PUBLIC,

    /** 登录后可访问。 */
    AUTHENTICATED,

    /** 仅创建人。 */
    OWNER,

    /** 仅管理员。 */
    ADMIN,

    /** 禁止访问。 */
    DENY
}
