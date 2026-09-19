package cn.miyf.auth.infrastructure.huawei;

/**
 * 用授权码换华为 openId / unionId。实现分 Mock 与真实换票，由配置切换。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
public interface HuaweiAccountClient {

    /**
     * 校验授权码并返回身份。
     *
     * @param authorizationCode Account Kit 授权码
     * @return 身份
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    HuaweiIdentity exchange(String authorizationCode);
}
