package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * S3 对象存储占位实现：接口已预留，当前未接入 SDK。
 * <p>
 * 配置 {@code app.file-storage.type=s3} 时启用，调用将返回 STORAGE_UNAVAILABLE。
 * MinIO 请使用 {@link MinioFileStorageService}。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Slf4j
public class ObjectStorageFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    /**
     * 构造对象存储占位服务。
     *
     * @param properties 配置
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public ObjectStorageFileStorageService(FileStorageProperties properties) {
        this.properties = properties;
        log.warn("Object storage type={} selected but SDK not wired yet (endpoint={}, bucket={})",
                properties.getType(), properties.getEndpoint(), properties.getBucket());
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public StoredFile store(InputStream inputStream, long size, String contentType, String path) {
        throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE,
                "对象存储（" + properties.getType() + "）尚未接入，请改用 local");
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public InputStream open(String path) {
        throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE,
                "对象存储（" + properties.getType() + "）尚未接入");
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public void delete(String path) {
        throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE,
                "对象存储（" + properties.getType() + "）尚未接入");
    }
}
