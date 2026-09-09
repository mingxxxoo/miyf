package cn.miyf.infrastructure.cache;

/**
 * 平台缓存 Key <b>规范</b>（不含业务具体 key）。
 * <p>
 * 格式：{@code ck:{域}:{资源}:...}<br>
 * 域常量见 {@link #DOMAIN_PERM} 等；具体 key 由各业务模块自行定义，并通过 {@link #join(Object...)} 拼接。
 * <pre>
 *   CacheKeys.join(CacheKeys.DOMAIN_DATA, "cat", "enabled")
 *   → ck:data:cat:enabled
 * </pre>
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
public final class CacheKeys {

    /** 全局根前缀。 */
    public static final String ROOT = "ck";

    /** 权限类（文件临时访问、数据权限等）。 */
    public static final String DOMAIN_PERM = "perm";

    /** 业务数据缓存。 */
    public static final String DOMAIN_DATA = "data";

    /** 认证防护（登录失败、验证码、锁定）。 */
    public static final String DOMAIN_AUTH = "auth";

    /** 分布式锁。 */
    public static final String DOMAIN_LOCK = "lock";

    /** 限流。 */
    public static final String DOMAIN_RATE = "rl";

    private CacheKeys() {
    }

    /**
     * 按规范拼接 key：{@code ck:a:b:c}。
     *
     * @param parts 段（不含根前缀；建议首段使用 DOMAIN_*）
     * @return 完整 key
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String join(Object... parts) {
        StringBuilder sb = new StringBuilder(ROOT);
        if (parts != null) {
            for (Object part : parts) {
                if (part == null) {
                    continue;
                }
                String text = String.valueOf(part).trim();
                if (text.isEmpty()) {
                    continue;
                }
                sb.append(':').append(text);
            }
        }
        return sb.toString();
    }
}
