package cn.miyf.auth.cache;

import cn.miyf.infrastructure.cache.CacheKeys;

/**
 * 认证模块缓存 Key（遵循 {@link CacheKeys} 规范）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public final class AuthCacheKeys {

    private AuthCacheKeys() {
    }

    /**
     * 登录失败计数。
     *
     * @param principal 登录主体
     * @return key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String loginFail(String principal) {
        return CacheKeys.join(CacheKeys.DOMAIN_AUTH, "login", "fail", principal);
    }

    /**
     * 登录临时锁定。
     *
     * @param principal 登录主体
     * @return key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String loginLock(String principal) {
        return CacheKeys.join(CacheKeys.DOMAIN_AUTH, "login", "lock", principal);
    }

    /**
     * 图形验证码。
     *
     * @param captchaId 验证码 ID
     * @return key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String captcha(String captchaId) {
        return CacheKeys.join(CacheKeys.DOMAIN_AUTH, "captcha", captchaId);
    }
}
