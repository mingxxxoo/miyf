package cn.miyf.organization.bean.dto;

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
 * 组织单位保存 DTO。
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
@Schema(name = "OrgUnitSaveDto", description = "组织单位保存")
public class OrgUnitSaveDto {

    @Schema(description = "父单位 ID", type = "string")
    private String parentId;

    @NotBlank
    @Schema(description = "编码")
    private String code;

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;
}
