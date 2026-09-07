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
 * 系统应用保存 DTO。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysAppSaveDto", description = "系统应用保存")
public class SysAppSaveDto {

    @NotBlank
    @Schema(description = "应用编码")
    private String code;

    @NotBlank
    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标名")
    private String icon;

    @Schema(description = "默认入口路径")
    private String homePath;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;
}
