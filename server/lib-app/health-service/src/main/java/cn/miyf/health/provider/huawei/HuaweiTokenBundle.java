package cn.miyf.health.provider.huawei;

import java.time.Instant;

/**
 * 华为 OAuth Token 缓存（仅存 token，不落 clientSecret）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class HuaweiTokenBundle {

    private String accessToken;
    private String refreshToken;
    private Instant expiresTime;
    private String openId;
    private String scope;
    /**
     * 凭证来源：oauth
     */
    private String source;

    public String getAccessToken() {
        return accessToken;
    }

    public HuaweiTokenBundle setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public HuaweiTokenBundle setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public Instant getExpiresTime() {
        return expiresTime;
    }

    public HuaweiTokenBundle setExpiresTime(Instant expiresTime) {
        this.expiresTime = expiresTime;
        return this;
    }

    public String getOpenId() {
        return openId;
    }

    public HuaweiTokenBundle setOpenId(String openId) {
        this.openId = openId;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public HuaweiTokenBundle setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getSource() {
        return source;
    }

    public HuaweiTokenBundle setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * access_token 是否仍可用：无过期时间则视为有效。
     */
    public boolean accessTokenValid(Instant now) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        if (expiresTime == null) {
            return true;
        }
        return expiresTime.isAfter(now.plusSeconds(60));
    }
}
