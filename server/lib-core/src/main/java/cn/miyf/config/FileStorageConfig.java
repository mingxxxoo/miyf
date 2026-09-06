package cn.miyf.config;

import cn.miyf.infrastructure.storage.FileStorageService;
import cn.miyf.infrastructure.storage.LocalFileStorageService;
import cn.miyf.infrastructure.storage.MinioFileStorageService;
import cn.miyf.infrastructure.storage.ObjectStorageFileStorageService;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 文件存储 Bean 装配：默认 MinIO，可选 local / s3 占位。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfig {

    /**
     * MinIO 客户端。
     *
     * @param properties 配置
     * @return 客户端
     * @history 1.00 2026-09-05 09:22 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient(FileStorageProperties properties) {
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalStateException("app.file-storage.endpoint 不能为空（MinIO）");
        }
        if (!StringUtils.hasText(properties.getAccessKey()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("app.file-storage.access-key / secret-key 不能为空（MinIO）");
        }
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * MinIO 存储（默认）。
     *
     * @param properties  配置
     * @param minioClient 客户端
     * @return 存储服务
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "minio", matchIfMissing = true)
    public FileStorageService minioFileStorageService(FileStorageProperties properties, MinioClient minioClient) {
        return new MinioFileStorageService(properties, minioClient);
    }

    /**
     * 本地磁盘存储。
     *
     * @param properties 配置
     * @return 存储服务
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "local")
    public FileStorageService localFileStorageService(FileStorageProperties properties) {
        return new LocalFileStorageService(properties);
    }

    /**
     * S3 占位（后续可复用 AWS SDK）。
     *
     * @param properties 配置
     * @return 存储服务
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    public FileStorageService s3FileStorageService(FileStorageProperties properties) {
        return new ObjectStorageFileStorageService(properties);
    }
}

