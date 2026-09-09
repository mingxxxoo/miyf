package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 上传文件校验：大小、Content-Type、魔数头。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Slf4j
public final class FileUploadValidator {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private FileUploadValidator() {
    }

    /**
     * 校验并探测真实图片类型；返回规范化 MIME。
     *
     * @param properties   配置
     * @param declaredType 客户端声明类型
     * @param header       文件头字节（至少 12 字节，不足则整段）
     * @param size         文件大小
     * @return 规范化 contentType
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public static String validateAndDetect(FileStorageProperties properties,
                                           String declaredType,
                                           byte[] header,
                                           long size) {
        if (size <= 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "空文件不可上传");
        }
        if (size > properties.getMaxSizeBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "文件不能超过 " + (properties.getMaxSizeBytes() / 1024 / 1024) + "MB");
        }
        String declared = normalizeMime(declaredType);
        Set<String> allowed = properties.getAllowedContentTypes() == null || properties.getAllowedContentTypes().isEmpty()
                ? ALLOWED
                : Set.copyOf(properties.getAllowedContentTypes().stream().map(FileUploadValidator::normalizeMime).toList());
        if (!StringUtils.hasText(declared) || !allowed.contains(declared)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "仅支持 JPG / PNG / WebP");
        }
        String magicType = detectByMagic(header);
        if (magicType == null || !allowed.contains(magicType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "文件内容与声明类型不符");
        }
        // 声明类型与魔数不一致时以魔数为准，但必须都在白名单
        if (!declared.equals(magicType)) {
            log.info("Content-Type mismatch declared={} magic={}, use magic", declared, magicType);
        }
        return magicType;
    }

    /**
     * 按魔数探测类型。
     *
     * @param header 文件头
     * @return MIME 或 null
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public static String detectByMagic(byte[] header) {
        if (header == null || header.length < 3) {
            return null;
        }
        // JPEG
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return "image/png";
        }
        // WEBP: RIFF....WEBP
        if (header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /**
     * 扩展名（含点）。
     *
     * @param contentType MIME
     * @return 扩展名
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public static String extensionOf(String contentType) {
        return switch (normalizeMime(contentType)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException(ErrorCode.INVALID_FILE);
        };
    }

    /**
     * 读取流前缀用于魔数检测，并返回可重放的完整流包装前的头+剩余需由调用方处理。
     *
     * @param inputStream 输入
     * @param maxHeader   读取头长度
     * @return 头字节
     * @throws IOException IO
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public static byte[] readHeader(InputStream inputStream, int maxHeader) throws IOException {
        return inputStream.readNBytes(maxHeader);
    }

    /**
     * 将已读头与剩余流拼成新流。
     *
     * @param header 头
     * @param rest   剩余
     * @return 完整流
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public static InputStream concat(byte[] header, InputStream rest) {
        return new SequenceInputStream(new ByteArrayInputStream(header), rest);
    }

    private static String normalizeMime(String mime) {
        if (!StringUtils.hasText(mime)) {
            return "";
        }
        String value = mime.trim().toLowerCase(Locale.ROOT);
        int semi = value.indexOf(';');
        if (semi > 0) {
            value = value.substring(0, semi).trim();
        }
        if ("image/jpg".equals(value)) {
            return "image/jpeg";
        }
        return value;
    }
}

