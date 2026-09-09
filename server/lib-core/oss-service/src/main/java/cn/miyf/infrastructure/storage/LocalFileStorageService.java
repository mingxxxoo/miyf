package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 本地磁盘文件存储实现。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Slf4j
public class LocalFileStorageService implements FileStorageService {

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
    public StoredFile store(InputStream inputStream, long size, String contentType, String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "存储路径不能为空");
        }
        try {
            byte[] header = FileUploadValidator.readHeader(inputStream, 16);
            String mime = FileUploadValidator.validateAndDetect(properties, contentType, header, size);
            Path target = resolveSafe(path);
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream full = FileUploadValidator.concat(header, inputStream);
                 DigestInputStream dig = new DigestInputStream(full, digest)) {
                Files.copy(dig, target, StandardCopyOption.REPLACE_EXISTING);
            }
            long actual = Files.size(target);
            if (actual > properties.getMaxSizeBytes()) {
                Files.deleteIfExists(target);
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
            }
            String md5 = HexFormat.of().formatHex(digest.digest());
            log.info("Stored local file path={} size={} md5={}", path, actual, md5);
            return new StoredFile(path, mime, actual, md5);
        } catch (BusinessException ex) {
            throw ex;
        } catch (NoSuchAlgorithmException | IOException ex) {
            log.error("Local store failed path={}", path, ex);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "本地存储失败");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public InputStream open(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        try {
            Path file = resolveSafe(path);
            if (!Files.isRegularFile(file)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
            }
            return Files.newInputStream(file);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Local open failed path={}", path, ex);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取本地文件失败");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public void delete(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(path));
        } catch (IOException ex) {
            log.warn("Delete local file failed path={}", path, ex);
        }
    }

    /**
     * 解析并防止路径穿越。
     *
     * @param path 相对路径
     * @return 绝对路径
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    Path resolveSafe(String path) {
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "非法文件路径");
        }
        return resolved;
    }

    Path getRoot() {
        return root;
    }
}
