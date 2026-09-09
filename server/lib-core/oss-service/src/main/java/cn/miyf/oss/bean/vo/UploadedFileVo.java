package cn.miyf.oss.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.security.FileAccess;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 文件上传结果 VO。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "UploadedFileVo", description = "上传结果")
public class UploadedFileVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文件 ID", type = "string")
    private Long id;

    @Schema(description = "存储路径")
    private String path;

    @FileAccess
    @Schema(description = "可访问 URL（/r/{id}）")
    private String url;

    @Schema(description = "Content-Type")
    private String contentType;

    @Schema(description = "字节大小")
    private Long size;

    @Schema(description = "内容 MD5")
    private String md5;

    @Schema(description = "产品应用编码")
    private String appCode;

    @Schema(description = "访问权限")
    private FileAccessPermission accessPermission;
}
