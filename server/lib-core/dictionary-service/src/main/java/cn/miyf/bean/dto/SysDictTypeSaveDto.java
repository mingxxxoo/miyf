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
 * 数据字典类型保存 DTO。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysDictTypeSaveDto", description = "数据字典类型保存")
public class SysDictTypeSaveDto {

    @NotBlank
    @Schema(description = "字典编码")
    private String code;

    @NotBlank
    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @Schema(description = "排序")
    private Integer sortOrder;
}
