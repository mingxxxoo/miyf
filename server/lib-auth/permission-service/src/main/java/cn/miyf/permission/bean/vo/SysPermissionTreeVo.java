package cn.miyf.permission.bean.vo;

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
 * 权限点树节点（无限极）。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysPermissionTreeVo", description = "权限树节点")
public class SysPermissionTreeVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "权限 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父权限 ID")
    private Long parentId;

    @Schema(description = "权限码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "权限组编码")
    private String groupCode;

    @Schema(description = "业务编号")
    private String permNo;

    @Schema(description = "产品域")
    private String product;

    @Schema(description = "层级展示名")
    private String treeName;

    @Schema(description = "节点类型 ROOT/PRODUCT/BIZ/API")
    private String nodeType;

    @Schema(description = "同级排序")
    private Integer sortOrder;

    @Schema(description = "子节点")
    private List<SysPermissionTreeVo> children = new ArrayList<>();
}
