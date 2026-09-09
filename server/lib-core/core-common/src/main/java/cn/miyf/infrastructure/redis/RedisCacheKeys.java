package cn.miyf.infrastructure.redis;

import cn.miyf.infrastructure.cache.CacheKeys;

/**
 * 已废弃：请使用 {@link CacheKeys} 规范 + 各模块具体 Key 类
 * （如 {@code AuthCacheKeys} / {@code KitchenCacheKeys} / {@code OssCacheKeys}）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 * @deprecated 见模块内 *CacheKeys
 */
@Deprecated
public final class RedisCacheKeys {

    private RedisCacheKeys() {
    }

    /**
     * 与认证模块登录失败计数 key 一致（core 限流器自用）。
     *
     * @param principal 主体
     * @return key
     * @deprecated 业务侧请用 AuthCacheKeys.loginFail
     */
    @Deprecated
    public static String loginFail(String principal) {
        return CacheKeys.join(CacheKeys.DOMAIN_AUTH, "login", "fail", principal);
    }
}
