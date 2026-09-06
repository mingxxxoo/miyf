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
 * 数据字典项保存 DTO。
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
@Schema(name = "SysDictItemSaveDto", description = "数据字典项保存")
public class SysDictItemSaveDto {

    @Schema(description = "字典类型 ID（创建时必填）")
    private String typeId;

    @NotBlank
    @Schema(description = "字典值")
    private String itemValue;

    @NotBlank
    @Schema(description = "显示标签")
    private String itemLabel;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
