package cn.miyf.auth.infrastructure.wx;

/**
 * 微信登录会话结果。
 *
 * @param openid  微信 openid
 * @param unionid 微信 unionid，可空
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
public record WxSession(String openid, String unionid) {
}
