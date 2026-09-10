package cn.miyf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置：默认 MinIO，可选本地 / S3。
 * 含对外 baseUrl、分区命名空间，以及签名 URL 专用密钥 accessSignSecret。
 * 签名有效期见系统配置 file.access.sign.ttl.seconds，不在本 Properties 中。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 * @history 1.00 2026-09-05 09:19 XieMingJie Created.
 */
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {

    /**
     * minio | local | s3
     */
    private String type = "minio";

    /**
     * 本地根目录（type=local）
     */
    private String path = "./uploads";

    /**
     * 对外访问站点前缀（不含 /r），公开 URL 为 {@code {baseUrl}/r/{fileId}}。
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * 存储路径命名空间前缀，如 miyf → miyf/kitchen/2026/09/{id}。
     */
    private String pathNamespace = "miyf";

    /**
     * 单文件最大字节数，默认 5MB
     */
    private long maxSizeBytes = 5 * 1024 * 1024L;

    /**
     * 允许的 Content-Type
     */
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg", "image/png", "image/webp"
    ));

    /**
     * MinIO API 地址，如 http://localhost:9000
     */
    private String endpoint = "http://localhost:9000";

    /**
     * 桶名
     */
    private String bucket = "miyf";

    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String region = "";

    /**
     * 启动时自动创建桶
     */
    private boolean autoCreateBucket = true;

    /**
     * 桶内对象公开可读（GetObject）。生产/容器环境须为 false，由应用鉴权或预签名访问。
     */
    private boolean publicRead = false;

    /**
     * 文件访问签名密钥（HMAC-SHA256），用于 {@code /r/{id}?exp=&sig=}。
     * 与 JWT secret、MinIO secret-key 分离；生产环境须通过 FILE_STORAGE_ACCESS_SIGN_SECRET 注入强随机值。
     * 签名有效期由系统配置 {@code file.access.sign.ttl.seconds} 热管理，不在此属性中配置。
     */
    private String accessSignSecret = "change-me-file-access-sign-secret-32chars";

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

    public String getPathNamespace() {
        return pathNamespace;
    }

    public void setPathNamespace(String pathNamespace) {
        this.pathNamespace = pathNamespace;
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

    public String getAccessSignSecret() {
        return accessSignSecret;
    }

    public void setAccessSignSecret(String accessSignSecret) {
        this.accessSignSecret = accessSignSecret;
    }
}

