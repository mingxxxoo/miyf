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
     * 文件临时访问权。
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
     * 文件临时访问权前缀（SCAN 失效）。
     *
     * @return 前缀
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String fileAccessPrefix() {
        return CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "access") + ":";
    }
}
