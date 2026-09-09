package cn.miyf.infrastructure.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 对象路径与公开 URL 工具。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:22
 */
public final class StoragePathUtils {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Pattern APP_CODE = Pattern.compile("^[a-z][a-z0-9_-]{0,62}$");

    private StoragePathUtils() {
    }

    /**
     * 校验并规范化应用编码。
     *
     * @param appCode 应用编码
     * @return 小写编码
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String requireAppCode(String appCode) {
        if (appCode == null || appCode.isBlank()) {
            throw new IllegalArgumentException("appCode 不能为空");
        }
        String code = appCode.trim().toLowerCase(Locale.ROOT);
        if (!APP_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("appCode 非法");
        }
        return code;
    }

    /**
     * 生成相对存储路径：{@code {namespace}/{appCode}/yyyy/MM/{fileId}}（无后缀）。
     *
     * @param namespace 命名空间，如 miyf
     * @param appCode   应用编码，如 kitchen / health
     * @param fileId    文件 ID
     * @return 相对路径
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String buildStoragePath(String namespace, String appCode, long fileId) {
        String ns = (namespace == null || namespace.isBlank()) ? "miyf" : namespace.trim().toLowerCase(Locale.ROOT);
        String app = requireAppCode(appCode);
        return ns + "/" + app + "/" + YEAR_MONTH.format(LocalDate.now()) + "/" + fileId;
    }

    /**
     * 公开访问 URL：{@code {baseUrl}/r/{fileId}}。
     *
     * @param baseUrl 站点前缀（不含 /r）
     * @param fileId  文件 ID
     * @return 完整 URL
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String publicResourceUrl(String baseUrl, long fileId) {
        return joinUrl(baseUrl, "r/" + fileId);
    }

    /**
     * 拼接 baseUrl 与相对路径。
     *
     * @param baseUrl 前缀
     * @param path    相对路径
     * @return 完整 URL
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    public static String joinUrl(String baseUrl, String path) {
        String b = baseUrl == null ? "" : baseUrl.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        String k = path == null ? "" : path;
        if (k.startsWith("/")) {
            k = k.substring(1);
        }
        return b + "/" + k;
    }
}
