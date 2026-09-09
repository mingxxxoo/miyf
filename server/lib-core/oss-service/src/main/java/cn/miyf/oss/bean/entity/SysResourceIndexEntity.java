package cn.miyf.oss.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import cn.miyf.oss.enums.FileAccessPermission;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 资源索引表（sys_resource_index）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_resource_index")
@Schema(name = "SysResourceIndexEntity", description = "资源索引")
public class SysResourceIndexEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("mime_type")
    @Schema(description = "文件类型 MIME")
    private String mimeType;

    @TableField("file_name")
    @Schema(description = "文件名称")
    private String fileName;

    @TableField("file_size")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("create_user")
    @Schema(description = "创建人员 ID", type = "string")
    private Long createUser;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("last_modify_user")
    @Schema(description = "最后修改人员 ID", type = "string")
    private Long lastModifyUser;

    @TableField("is_compress")
    @Schema(description = "是否压缩")
    private Boolean compress;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("rm_id")
    @Schema(description = "所属存储服务（预留）", type = "string")
    private Long rmId;

    @TableField("path")
    @Schema(description = "存储路径")
    private String path;

    @TableField("is_temp")
    @Schema(description = "是否临时文件")
    private Boolean temp;

    @TableField("source")
    @Schema(description = "来源")
    private String source;

    @TableField("md5")
    @Schema(description = "内容 MD5")
    private String md5;

    @TableField("app_code")
    @Schema(description = "产品应用编码")
    private String appCode;

    @TableField("access_permission")
    @Schema(description = "文件访问权限")
    private FileAccessPermission accessPermission;
}
