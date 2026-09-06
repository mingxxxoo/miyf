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
 * 组织单位实体（sys_org_unit）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_org_unit")
@Schema(name = "SysOrgUnitEntity", description = "组织单位")
public class SysOrgUnitEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    @Schema(description = "父单位 ID")
    private Long parentId;

    @TableField("code")
    @Schema(description = "单位编码")
    private String code;

    @TableField("name")
    @Schema(description = "单位名称")
    private String name;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("status")
    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
