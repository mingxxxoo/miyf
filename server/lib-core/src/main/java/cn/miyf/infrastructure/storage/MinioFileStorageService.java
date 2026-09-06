package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * MinIO 对象存储实现。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:22
 */
public class MinioFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageService.class);

    private final FileStorageProperties properties;
    private final MinioClient minioClient;

    /**
     * 构造 MinIO 存储；可选自动建桶并开放公开读。
     *
     * @param properties  配置
     * @param minioClient 客户端
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    public MinioFileStorageService(FileStorageProperties properties, MinioClient minioClient) {
        this.properties = properties;
        this.minioClient = minioClient;
        ensureBucket();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    @Override
    public StoredFile store(InputStream inputStream, long size, String contentType, String originalName) {
        try {
            byte[] header = FileUploadValidator.readHeader(inputStream, 16);
            String mime = FileUploadValidator.validateAndDetect(properties, contentType, header, size);
            String objectKey = StoragePathUtils.nextObjectKey(mime);
            try (InputStream full = FileUploadValidator.concat(header, inputStream)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .stream(full, size, -1)
                        .contentType(mime)
                        .build());
            }
            String url = StoragePathUtils.joinUrl(properties.getBaseUrl(), objectKey);
            log.info("Stored minio object bucket={} key={} size={} original={}",
                    properties.getBucket(), objectKey, size, originalName);
            return new StoredFile(objectKey, url, mime, size);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("MinIO store failed bucket={}", properties.getBucket(), ex);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "MinIO 存储失败");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("MinIO delete failed key={}", objectKey, ex);
        }
    }

    private void ensureBucket() {
        if (!properties.isAutoCreateBucket()) {
            return;
        }
        String bucket = properties.getBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("app.file-storage.bucket 不能为空");
        }
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket {}", bucket);
            }
            if (properties.isPublicRead()) {
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": ["*"]},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucket);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("初始化 MinIO bucket 失败: " + bucket, ex);
        }
    }
}
