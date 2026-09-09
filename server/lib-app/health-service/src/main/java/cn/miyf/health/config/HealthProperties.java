package cn.miyf.health.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康管理模块配置（设备/数据源接入可扩展）。
 * <p>
 * 华为作为其中一个 {@code providers.huawei} 实现，不改变模块通用定位。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@ConfigurationProperties(prefix = "app.health")
public class HealthProperties {

    /**
     * 是否启用健康模块
     */
    private boolean enabled = true;

    /**
     * 数据源开关：key = provider code（manual / example / huawei）。
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * 华为 Health Kit 专用配置
     */
    private final Huawei huawei = new Huawei();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers != null ? providers : new HashMap<>();
    }

    public Huawei getHuawei() {
        return huawei;
    }

    public static class ProviderConfig {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 华为运动健康服务（Health Kit）REST 接入。
     */
    public static class Huawei {
        /**
         * 为 true 时不调真实华为接口，返回演示采样（本地联调）
         */
        private boolean mockEnabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:5173/health/providers";
        private String authorizeUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize";
        private String tokenUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/token";
        private String healthApiBase = "https://health-api.cloud.huawei.com/healthkit/v1";
        private List<String> scopes = new ArrayList<>(List.of(
                "openid",
                "https://www.huawei.com/healthkit/heightweight.read",
                "https://www.huawei.com/healthkit/heartrate.read",
                "https://www.huawei.com/healthkit/step.read",
                "https://www.huawei.com/healthkit/bloodglucose.read",
                "https://www.huawei.com/healthkit/bloodpressure.read",
                "https://www.huawei.com/healthkit/historydata.open.week"
        ));

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

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public void setAuthorizeUrl(String authorizeUrl) {
            this.authorizeUrl = authorizeUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getHealthApiBase() {
            return healthApiBase;
        }

        public void setHealthApiBase(String healthApiBase) {
            this.healthApiBase = healthApiBase;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes != null ? scopes : new ArrayList<>();
        }
    }
}
