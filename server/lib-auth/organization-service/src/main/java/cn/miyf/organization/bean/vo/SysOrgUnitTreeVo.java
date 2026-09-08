package cn.miyf.organization.bean.vo;

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
 * 组织单位树节点（无限极）。
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
@Schema(name = "SysOrgUnitTreeVo", description = "组织单位树节点")
public class SysOrgUnitTreeVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "单位 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父单位 ID")
    private Long parentId;

    @Schema(description = "单位编码")
    private String code;

    @Schema(description = "单位名称")
    private String name;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @Schema(description = "子单位")
    private List<SysOrgUnitTreeVo> children = new ArrayList<>();
}
