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
 * 菜单实体（sys_menu）。
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
@TableName("sys_menu")
@Schema(name = "SysMenuEntity", description = "系统菜单")
public class SysMenuEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    @Schema(description = "父菜单 ID")
    private Long parentId;

    @TableField("name")
    @Schema(description = "菜单名称")
    private String name;

    @TableField("path")
    @Schema(description = "路由路径")
    private String path;

    @TableField("component")
    @Schema(description = "前端组件")
    private String component;

    @TableField("icon")
    @Schema(description = "图标")
    private String icon;

    @TableField("menu_type")
    @Schema(description = "类型：DIR/MENU/BUTTON")
    private String menuType;

    @TableField("permission_code")
    @Schema(description = "绑定权限码")
    private String permissionCode;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("visible")
    @Schema(description = "是否可见")
    private Boolean visible;

    @TableField("status")
    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
