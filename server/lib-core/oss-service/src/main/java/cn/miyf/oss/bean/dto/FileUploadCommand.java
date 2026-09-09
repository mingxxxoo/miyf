package cn.miyf.oss.bean.dto;

import cn.miyf.oss.enums.FileAccessPermission;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件上传业务参数。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Schema(name = "FileUploadCommand", description = "文件上传参数")
public class FileUploadCommand {

    @Schema(description = "产品应用编码，如 kitchen / health", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "是否临时文件")
    private boolean temp;

    @Schema(description = "是否压缩")
    private boolean compress;

    @Schema(description = "访问权限，默认 OWNER（仅创建人/管理员经鉴权访问）")
    private FileAccessPermission accessPermission = FileAccessPermission.OWNER;

    public String getAppCode() {
        return appCode;
    }

    public FileUploadCommand setAppCode(String appCode) {
        this.appCode = appCode;
        return this;
    }

    public String getSource() {
        return source;
    }

    public FileUploadCommand setSource(String source) {
        this.source = source;
        return this;
    }

    public boolean isTemp() {
        return temp;
    }

    public FileUploadCommand setTemp(boolean temp) {
        this.temp = temp;
        return this;
    }

    public boolean isCompress() {
        return compress;
    }

    public FileUploadCommand setCompress(boolean compress) {
        this.compress = compress;
        return this;
    }

    public FileAccessPermission getAccessPermission() {
        return accessPermission;
    }

    public FileUploadCommand setAccessPermission(FileAccessPermission accessPermission) {
        this.accessPermission = accessPermission;
        return this;
    }
}
