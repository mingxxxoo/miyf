package cn.miyf.auth.infrastructure.huawei;

/**
 * 华为账号换票后的身份，不含 access token。
 *
 * @param openId      华为 openId
 * @param unionId     华为 unionId，跨应用合并的主键
 * @param displayName 昵称，可空
 * @param phone       手机号，可空；仅在授权了手机号范围时有值
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
public record HuaweiIdentity(String openId, String unionId, String displayName, String phone) {
}
