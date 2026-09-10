package cn.miyf.oss.security;

import cn.miyf.config.FileStorageProperties;
import cn.miyf.service.SystemConfigReader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件短期访问签名：{@code /r/{id}?exp={unix}&sig={hmac}}，供 img / 小程序 Image 等无法携带 JWT 的请求使用。
 * <p>
 * 签名载荷为 {@code "{fileId}:{exp}"}，算法 HMAC-SHA256；
 * 密钥取自 {@code app.file-storage.access-sign-secret}；
 * 有效期每次签发时从系统配置 {@link #CONFIG_SIGN_TTL_SECONDS} 热读取（管理端可随时修改）。
 * 篡改 exp 或 fileId 会导致 sig 校验失败。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Component
public class FileAccessSigner {

    public static final String PARAM_EXP = "exp";
    public static final String PARAM_SIG = "sig";

    /** 系统配置键：签名 URL 有效期（秒），对应 sys_config。 */
    public static final String CONFIG_SIGN_TTL_SECONDS = "file.access.sign.ttl.seconds";

    /** 系统配置缺失或非法时的兜底有效期（秒）。 */
    public static final long FALLBACK_TTL_SECONDS = 5 * 3600L;

    private static final Pattern FILE_ID_IN_URL = Pattern.compile("/r/(\\d+)(?:\\D|$)");
    private static final HexFormat HEX = HexFormat.of();

    private final byte[] secretBytes;
    private final FileAccessPermissionCache fileAccessPermissionCache;
    private final SystemConfigReader systemConfigReader;

    /**
     * 注入签名密钥与系统配置读取器；TTL 不在构造时固化。
     *
     * @param fileStorageProperties     含 access-sign-secret
     * @param fileAccessPermissionCache 文件 ID 解析
     * @param systemConfigReader        系统配置热读
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public FileAccessSigner(FileStorageProperties fileStorageProperties,
                            FileAccessPermissionCache fileAccessPermissionCache,
                            SystemConfigReader systemConfigReader) {
        String secret = fileStorageProperties == null ? null : fileStorageProperties.getAccessSignSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.file-storage.access-sign-secret 不能为空（文件访问签名）");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.fileAccessPermissionCache = fileAccessPermissionCache;
        this.systemConfigReader = systemConfigReader;
    }

    /**
     * 计算 HMAC 签名（hex）。
     *
     * @param fileId 文件 ID
     * @param exp    过期 unix 秒
     * @return hex 签名
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public String sign(long fileId, long exp) {
        return hmacHex(payload(fileId, exp));
    }

    /**
     * 校验签名是否有效且未过期。
     * 使用恒定时间比较，避免通过耗时差异猜测签名。
     *
     * @param fileId 文件 ID
     * @param exp    过期 unix 秒（可为 null）
     * @param sig    签名（可为 null）
     * @return true 表示放行
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public boolean verify(Long fileId, Long exp, String sig) {
        if (fileId == null || exp == null || !StringUtils.hasText(sig)) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            return false;
        }
        String expected = sign(fileId, exp);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = sig.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    /**
     * 将 URL / 纯数字 ID 改写为带签名的访问地址；无法解析则原样返回。
     * <p>
     * 若已带未过期有效签名则不再改写。有效期取自系统配置 {@link #CONFIG_SIGN_TTL_SECONDS}。
     *
     * @param raw 原始值
     * @return 签名 URL 或原值
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public String signUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        Optional<Long> fileIdOpt = fileAccessPermissionCache.parseFileId(raw);
        if (fileIdOpt.isEmpty()) {
            return raw;
        }
        long fileId = fileIdOpt.get();
        QueryParams existing = parseQuery(raw);
        if (verify(fileId, existing.exp(), existing.sig())) {
            return raw;
        }
        String base = stripQueryAndFragment(raw.trim());
        if (base.chars().allMatch(Character::isDigit)) {
            base = "/r/" + fileId;
        }
        long exp = Instant.now().getEpochSecond() + resolveTtlSeconds();
        String sig = sign(fileId, exp);
        return base + "?" + PARAM_EXP + "=" + exp + "&" + PARAM_SIG + "=" + sig;
    }

    /**
     * 若值为可解析的文件引用则签名改写，否则原样返回。
     *
     * @param value 字段值
     * @return 可能已签名的值
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public Object signValue(Object value) {
        if (value instanceof String text) {
            return signUrl(text);
        }
        if (value instanceof Number number) {
            return signUrl(String.valueOf(number.longValue()));
        }
        return value;
    }

    /**
     * 读取当前签名有效期（秒）；配置缺失、非正数时回退兜底值。
     *
     * @return TTL 秒数
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    long resolveTtlSeconds() {
        if (systemConfigReader == null) {
            return FALLBACK_TTL_SECONDS;
        }
        long ttl = systemConfigReader.getLong(CONFIG_SIGN_TTL_SECONDS, FALLBACK_TTL_SECONDS);
        return ttl > 0 ? ttl : FALLBACK_TTL_SECONDS;
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("文件访问签名失败", ex);
        }
    }

    private static String payload(long fileId, long exp) {
        return fileId + ":" + exp;
    }

    private static String stripQueryAndFragment(String url) {
        int q = url.indexOf('?');
        int h = url.indexOf('#');
        int cut = url.length();
        if (q >= 0) {
            cut = Math.min(cut, q);
        }
        if (h >= 0) {
            cut = Math.min(cut, h);
        }
        return url.substring(0, cut);
    }

    private static QueryParams parseQuery(String url) {
        int q = url.indexOf('?');
        if (q < 0 || q == url.length() - 1) {
            return QueryParams.EMPTY;
        }
        String query = url.substring(q + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) {
            query = query.substring(0, hash);
        }
        Long exp = null;
        String sig = null;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq);
            String val = part.substring(eq + 1);
            if (PARAM_EXP.equals(key)) {
                try {
                    exp = Long.parseLong(val);
                } catch (NumberFormatException ignored) {
                    exp = null;
                }
            } else if (PARAM_SIG.equals(key)) {
                sig = val;
            }
        }
        return new QueryParams(exp, sig);
    }

    /**
     * 从路径片段提取文件 ID（与 {@link FileAccessPermissionCache#parseFileId} 互补）。
     *
     * @param path 路径
     * @return 文件 ID
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    static Optional<Long> extractFileIdFromPath(String path) {
        if (!StringUtils.hasText(path)) {
            return Optional.empty();
        }
        Matcher matcher = FILE_ID_IN_URL.matcher(path);
        if (matcher.find()) {
            return Optional.of(Long.parseLong(matcher.group(1)));
        }
        return Optional.empty();
    }

    private record QueryParams(Long exp, String sig) {
        private static final QueryParams EMPTY = new QueryParams(null, null);
    }
}
