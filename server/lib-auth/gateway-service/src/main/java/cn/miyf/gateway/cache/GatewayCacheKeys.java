package cn.miyf.gateway.cache;

import cn.miyf.infrastructure.cache.CacheKeys;

/**
 * 网关模块缓存 Key（遵循 {@link CacheKeys} 规范）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public final class GatewayCacheKeys {

    private GatewayCacheKeys() {
    }

    /**
     * API 限流。
     *
     * @param identity IP 或用户标识
     * @return key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String apiRate(String identity) {
        return CacheKeys.join(CacheKeys.DOMAIN_RATE, "api", identity);
    }
}
