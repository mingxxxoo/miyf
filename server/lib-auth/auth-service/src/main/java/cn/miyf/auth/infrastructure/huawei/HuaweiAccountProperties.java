package cn.miyf.auth.infrastructure.huawei;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 华为账号登录配置。clientSecret 只允许出现在服务端环境变量。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@ConfigurationProperties(prefix = "app.auth.huawei")
public class HuaweiAccountProperties {

    /**
     * 开发环境用授权码映射本地用户；生产与容器必须为 false。
     */
    private boolean mockEnabled = false;
    private String clientId = "";
    private String clientSecret = "";
    private String tokenUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/token";
    private String tokenInfoUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/tokeninfo";
    private String userInfoUrl = "https://account.cloud.huawei.com/rest.php";

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getTokenInfoUrl() {
        return tokenInfoUrl;
    }

    public void setTokenInfoUrl(String tokenInfoUrl) {
        this.tokenInfoUrl = tokenInfoUrl;
    }

    public String getUserInfoUrl() {
        return userInfoUrl;
    }

    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }
}
