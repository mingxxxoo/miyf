package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 创建/更新菜谱请求。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "RecipeSaveDto", description = "菜谱保存请求")
public class RecipeSaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @Schema(description = "菜谱描述")
    private String description;

    @Schema(description = "难度：EASY/MEDIUM/HARD")
    private String difficulty;

    @Schema(description = "准备分钟数")
    private Integer prepareMinutes;

    @Schema(description = "烹饪分钟数")
    private Integer cookMinutes;

    @Schema(description = "建议份量")
    private Integer servings;

    @Valid
    @Schema(description = "食材列表")
    private List<RecipeMaterialDto> ingredients;

    @Valid
    @Schema(description = "调味料列表")
    private List<RecipeMaterialDto> seasonings;

    @Valid
    @Schema(description = "制作步骤")
    private List<RecipeStepDto> steps;

    @Schema(description = "小贴士")
    private String tips;

    @Schema(description = "营养信息，仅允许标量键值")
    private Map<String, Object> nutrition;
}
