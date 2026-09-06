package cn.miyf.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HMAC 密钥，生产环境必须通过环境变量注入
     */
    private String secret = "change-me-to-a-long-random-secret-at-least-32-chars";
    /**
     * 过期秒数，默认 7 天
     */
    private long expireSeconds = 604800L;

    /**
     * 获取密钥。
     *
     * @return secret
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置密钥。
     *
     * @param secret 密钥
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 获取过期秒数。
     *
     * @return 过期秒数
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 设置过期秒数。
     *
     * @param expireSeconds 过期秒数
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
