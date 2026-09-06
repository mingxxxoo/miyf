package cn.miyf.bean.vo;

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

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
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
@Schema(name = "SysMenuTreeVo", description = "菜单树节点")
public class SysMenuTreeVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜单 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父菜单 ID")
    private Long parentId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "组件")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "类型 DIR/MENU/BUTTON")
    private String menuType;

    @Schema(description = "权限码")
    private String permissionCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "子节点")
    private List<SysMenuTreeVo> children = new ArrayList<>();
}
