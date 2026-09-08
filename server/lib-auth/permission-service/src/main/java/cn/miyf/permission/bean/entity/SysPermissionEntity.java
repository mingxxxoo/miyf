package cn.miyf.permission.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
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
 * 权限实体（sys_permission）。
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
@TableName("sys_permission")
@Schema(name = "SysPermissionEntity", description = "权限")
public class SysPermissionEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "权限码（JWT/鉴权用）")
    private String code;

    @TableField("name")
    @Schema(description = "权限名称")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("group_code")
    @Schema(description = "所属权限组编码（位或历史编码）")
    private String groupCode;

    @TableField("perm_no")
    @Schema(description = "16位业务编号")
    private String permNo;

    @TableField("parent_id")
    @Schema(description = "父权限 ID", type = "string")
    private Long parentId;

    @TableField("product")
    @Schema(description = "产品域")
    private String product;

    @TableField("tree_name")
    @Schema(description = "层级展示名")
    private String treeName;

    @TableField("node_type")
    @Schema(description = "节点类型 ROOT/PRODUCT/BIZ/API")
    private String nodeType;

    @TableField("sort_order")
    @Schema(description = "同级排序")
    private Integer sortOrder;
}
