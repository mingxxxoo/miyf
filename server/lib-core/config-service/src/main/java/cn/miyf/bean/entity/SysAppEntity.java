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
 * 系统应用实体（sys_app）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_app")
@Schema(name = "SysAppEntity", description = "系统应用")
public class SysAppEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "应用编码")
    private String code;

    @TableField("name")
    @Schema(description = "应用名称")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("icon")
    @Schema(description = "图标名")
    private String icon;

    @TableField("home_path")
    @Schema(description = "默认入口路径")
    private String homePath;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("status")
    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @TableField("built_in")
    @Schema(description = "是否内置（不可删）")
    private Boolean builtIn;

    @TableField(exist = false)
    @Schema(description = "权限数量（按 product=code 统计）")
    private Integer permissionCount;

    @TableField(exist = false)
    @Schema(description = "菜单数量（按 product=code，DIR/MENU）")
    private Integer menuCount;
}
