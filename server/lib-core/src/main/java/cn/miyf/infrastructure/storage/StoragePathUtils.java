package cn.miyf.infrastructure.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 对象键与公开 URL 工具。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:22
 */
public final class StoragePathUtils {

    private static final DateTimeFormatter DAY_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private StoragePathUtils() {
    }

    /**
     * 生成服务端对象键：日期目录 + Long + 扩展名。
     *
     * @param contentType MIME
     * @return 对象键
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    public static String nextObjectKey(String contentType) {
        String ext = FileUploadValidator.extensionOf(contentType);
        return DAY_DIR.format(LocalDate.now()) + "/"
                + Long.toHexString(System.nanoTime())
                + Integer.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextInt())
                + ext;
    }

    /**
     * 拼接 baseUrl 与 objectKey。
     *
     * @param baseUrl 前缀
     * @param objectKey 对象键
     * @return 完整 URL
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    public static String joinUrl(String baseUrl, String objectKey) {
        String b = baseUrl == null ? "" : baseUrl.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        String k = objectKey == null ? "" : objectKey;
        if (k.startsWith("/")) {
            k = k.substring(1);
        }
        return b + "/" + k;
    }
}

