package cn.miyf.kitchen.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜谱食材/调味料条目。
 * <p>
 * 组织协作做菜新增 unit/processing/required/substitute/note，用于做菜模板；
 * 老数据只会带 name/amount，新字段为空。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "RecipeMaterialDto", description = "食材或调味料")
public class RecipeMaterialDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "用量，AI 无法确认时可为空")
    private String amount;

    @Schema(description = "单位，如 g/ml/个")
    private String unit;

    @Schema(description = "处理方式，如切丁、去壳")
    private String processing;

    @Schema(description = "是否必需，默认 true")
    private Boolean required;

    @Schema(description = "替代方案")
    private String substitute;

    @Schema(description = "备注")
    private String note;

    /**
     * 兼容旧代码的二元构造（仅名称与用量）。
     *
     * @param name   名称
     * @param amount 用量
     */
    public RecipeMaterialDto(String name, String amount) {
        this.name = name;
        this.amount = amount;
    }
}
