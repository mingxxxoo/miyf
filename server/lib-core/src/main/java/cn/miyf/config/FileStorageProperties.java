package cn.miyf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置：默认 MinIO，可选本地 / S3。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {

    /** minio | local | s3 */
    private String type = "minio";

    /** 本地根目录（type=local） */
    private String path = "./uploads";

    /**
     * 对外访问前缀。
     * MinIO 默认 path-style：http://localhost:9000/{bucket}
     */
    private String baseUrl = "http://localhost:9000/miyf";

    /** 单文件最大字节数，默认 5MB */
    private long maxSizeBytes = 5 * 1024 * 1024L;

    /** 允许的 Content-Type */
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg", "image/png", "image/webp"
    ));

    /** MinIO API 地址，如 http://localhost:9000 */
    private String endpoint = "http://localhost:9000";

    /** 桶名 */
    private String bucket = "miyf";

    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String region = "";

    /** 启动时自动创建桶 */
    private boolean autoCreateBucket = true;

    /** 桶内对象公开可读（GetObject） */
    private boolean publicRead = true;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isAutoCreateBucket() {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket) {
        this.autoCreateBucket = autoCreateBucket;
    }

    public boolean isPublicRead() {
        return publicRead;
    }

    public void setPublicRead(boolean publicRead) {
        this.publicRead = publicRead;
    }
}

