package cn.miyf.oss.cache;

import cn.miyf.infrastructure.cache.CacheKeys;

/**
 * OSS 模块缓存 Key（遵循 {@link CacheKeys} 规范）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public final class OssCacheKeys {

    private OssCacheKeys() {
    }

    /**
     * 按登录主体的文件临时访问权。
     * {@code ck:perm:file:access:{principalType}:{principalId}:{fileId}}
     *
     * @param principalType 主体类型小写
     * @param principalId   主体 ID
     * @param fileId        文件 ID
     * @return key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String fileAccess(String principalType, long principalId, long fileId) {
        return CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "access", principalType, principalId, fileId);
    }

    /**
     * 按文件 ID 的短期可读票据（业务响应 {@code @FileAccess} 时发放，img 直开无需 query/path 凭证）。
     * {@code ck:perm:file:ticket:{fileId}}
     *
     * @param fileId 文件 ID
     * @return key
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public static String fileAccessTicket(long fileId) {
        return CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "ticket", fileId);
    }

    /**
     * 文件临时访问权前缀（SCAN 失效）。
     *
     * @return 前缀
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String fileAccessPrefix() {
        return CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "access") + ":";
    }

    /**
     * 文件可读票据前缀。
     *
     * @return 前缀
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public static String fileAccessTicketPrefix() {
        return CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "ticket") + ":";
    }
}
