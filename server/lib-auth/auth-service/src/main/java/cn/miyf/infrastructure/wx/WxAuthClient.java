package cn.miyf.infrastructure.wx;

/**
 * 微信鉴权客户端：用 code 换取 openid。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
public interface WxAuthClient {

    /**
     * 用小程序 login code 换取会话。
     *
     * @param code 微信临时登录凭证
     * @return 会话
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    WxSession code2Session(String code);
}
