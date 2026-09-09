package cn.miyf.auth.infrastructure.wx;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信登录配置（仅服务端）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@ConfigurationProperties(prefix = "wx.auth")
public class WxAuthProperties {

    private String appId = "";
    private String appSecret = "";
    /**
     * 开发环境可在 application-dev.yml 开启 Mock；生产/容器必须为 false
     */
    private boolean mockEnabled = false;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }
}
