package cn.miyf.bean.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 系统配置实体（sys_config）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_config")
@Schema(name = "SysConfigEntity", description = "系统配置")
public class SysConfigEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("config_key")
    @Schema(description = "配置键")
    private String configKey;

    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;

    @TableField("is_sensitive")
    @Schema(description = "是否敏感（脱敏返回）")
    private Boolean sensitive;

    @TableField("value_type")
    @Schema(description = "值类型 STRING/NUMBER/BOOLEAN/JSON")
    private String valueType;

    @TableField("group_code")
    @Schema(description = "分组编码")
    private String groupCode;

    @TableField("name")
    @Schema(description = "显示名")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("status")
    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
}
