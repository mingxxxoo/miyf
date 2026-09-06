package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
/**
 * 本地磁盘文件存储实现。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final FileStorageProperties properties;
    private final Path root;

    /**
     * 构造本地存储，并确保根目录存在。
     *
     * @param properties 配置
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public LocalFileStorageService(FileStorageProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + this.root, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public StoredFile store(InputStream inputStream, long size, String contentType, String originalName) {
        try {
            byte[] header = FileUploadValidator.readHeader(inputStream, 16);
            String mime = FileUploadValidator.validateAndDetect(properties, contentType, header, size);
            String objectKey = StoragePathUtils.nextObjectKey(mime);
            Path target = resolveSafe(objectKey);
            Files.createDirectories(target.getParent());
            try (InputStream full = FileUploadValidator.concat(header, inputStream)) {
                Files.copy(full, target, StandardCopyOption.REPLACE_EXISTING);
            }
            long actual = Files.size(target);
            if (actual != size && size > 0) {
                // multipart 声明的 size 可能与实际略有偏差时以落盘为准，但仍需不超过上限
                if (actual > properties.getMaxSizeBytes()) {
                    Files.deleteIfExists(target);
                    throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                }
            }
            String url = StoragePathUtils.joinUrl(properties.getBaseUrl(), objectKey);
            log.info("Stored local file key={} size={} original={}", objectKey, actual, originalName);
            return new StoredFile(objectKey, url, mime, actual);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Local store failed", ex);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "本地存储失败");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(objectKey));
        } catch (IOException ex) {
            log.warn("Delete local file failed key={}", objectKey, ex);
        }
    }

    /**
     * 解析并防止路径穿越。
     *
     * @param objectKey 对象键
     * @return 绝对路径
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    Path resolveSafe(String objectKey) {
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "非法文件路径");
        }
        return resolved;
    }

    Path getRoot() {
        return root;
    }
}

