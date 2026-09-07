package cn.miyf.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 菜单保存 DTO。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysMenuSaveDto", description = "菜单保存")
public class SysMenuSaveDto {

    @Schema(description = "父菜单 ID", type = "string")
    private String parentId;

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "组件")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @NotBlank
    @Schema(description = "类型 DIR/MENU/BUTTON")
    private String menuType;

    @Schema(description = "权限码")
    private String permissionCode;

    @Schema(description = "产品域")
    private String product;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "状态")
    private String status;
}
