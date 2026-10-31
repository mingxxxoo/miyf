package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 菜品模板（候选菜）保存请求。
 * <p>
 * 一次提交基础信息、原材料与步骤；服务端按列表顺序重排步骤，并汇总预计总时长。
 * 草稿/预创建由 {@link #targetStatus} 决定，发布与归档走独立接口。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DishTemplateSaveDto", description = "菜品模板保存请求")
public class DishTemplateSaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "菜品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 512)
    @Schema(description = "封面图 URL")
    private String coverImage;

    @Size(max = 2000)
    @Schema(description = "菜品介绍")
    private String description;

    @Min(1)
    @Max(99)
    @Schema(description = "建议份数，默认 1")
    private Integer servings;

    @Schema(description = "难度：EASY/MEDIUM/HARD")
    private String difficulty;

    @Size(max = 10)
    @Schema(description = "标签，最多 10 个")
    private List<@Size(max = 16) String> tags;

    @Valid
    @Schema(description = "原材料")
    private List<RecipeMaterialDto> ingredients;

    @Valid
    @Schema(description = "做菜步骤")
    private List<RecipeStepDto> steps;

    @Schema(description = "来源：MANUAL/AI_TEXT/AI_IMAGE/AI_URL")
    private String sourceType;

    @Size(max = 1024)
    @Schema(description = "来源网页地址")
    private String sourceUrl;

    @Schema(description = "保存状态：draft/pre_created，默认 draft")
    private String targetStatus;
}
