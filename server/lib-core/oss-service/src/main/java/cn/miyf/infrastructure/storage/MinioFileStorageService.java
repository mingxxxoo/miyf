package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * MinIO 对象存储实现。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:22
 */
@Slf4j
public class MinioFileStorageService implements FileStorageService {

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
    public StoredFile store(InputStream inputStream, long size, String contentType, String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "存储路径不能为空");
        }
        try {
            byte[] header = FileUploadValidator.readHeader(inputStream, 16);
            String mime = FileUploadValidator.validateAndDetect(properties, contentType, header, size);
            String storedPath = StoragePathUtils.withContentExtension(path, mime);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream full = FileUploadValidator.concat(header, inputStream);
                 DigestInputStream dig = new DigestInputStream(full, digest)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(storedPath)
                        .stream(dig, size, -1)
                        .contentType(mime)
                        .build());
            }
            String md5 = HexFormat.of().formatHex(digest.digest());
            log.info("Stored minio object bucket={} path={} size={} md5={}",
                    properties.getBucket(), storedPath, size, md5);
            return new StoredFile(storedPath, mime, size, md5);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("MinIO store failed bucket={} path={}", properties.getBucket(), path, ex);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "MinIO 存储失败");
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
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(path)
                    .build());
        } catch (Exception ex) {
            log.warn("MinIO open failed path={}", path, ex);
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    @Override
    public void delete(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(path)
                    .build());
        } catch (Exception ex) {
            log.warn("MinIO delete failed path={}", path, ex);
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
