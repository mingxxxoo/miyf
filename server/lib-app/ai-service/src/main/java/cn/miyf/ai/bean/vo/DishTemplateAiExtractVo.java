package cn.miyf.ai.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品库 AI 建菜确认页 VO。
 * <p>
 * AI 只生成草稿：服务端不落库、不自动发布，由成员在确认页逐项核对后
 * 走现有 {@code /dishes/templates} 保存接口。{@code uncertainFields} 标出需要重点核对的字段，
 * {@code reviewHints} 给出可直接展示的中文提示。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DishTemplateAiExtractVo", description = "菜品库 AI 建菜抽取结果")
public class DishTemplateAiExtractVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否降级（AI 不可用或调用失败）")
    private boolean degraded;

    @Schema(description = "降级/拒绝原因，正常识别时为 null")
    private String rejectReason;

    @Schema(description = "来源：AI_TEXT / AI_IMAGE / AI_URL")
    private String sourceType;

    @Schema(description = "来源网页地址，仅 AI_URL 有值")
    private String sourceUrl;

    @Schema(description = "封面图 URL（图片识别时为本次上传图）")
    private String coverImage;

    @Schema(description = "整体置信度：HIGH / MEDIUM / LOW")
    private String overallConfidence;

    @Schema(description = "菜品草稿")
    private DishDraftVo dish = new DishDraftVo();

    @Schema(description = "需要重点核对的字段")
    private List<UncertainFieldVo> uncertainFields = new ArrayList<>();

    @Schema(description = "确认页提示")
    private List<String> reviewHints = new ArrayList<>();

    @Schema(description = "服务端是否截断过超长内容")
    private boolean aiTruncated;

    /**
     * 单道菜草稿：原材料与步骤直接复用保存接口的 DTO 结构，确认页可原样提交。
     *
     * @author XieMingJie
     * @since 2026-10-31 18:00
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DishTemplateAiDishDraftVo", description = "菜品草稿")
    public static class DishDraftVo implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String name;
        private String description;
        private Integer servings;
        private String difficulty;
        private List<String> tags = new ArrayList<>();
        private List<RecipeMaterialDto> ingredients = new ArrayList<>();
        private List<RecipeStepDto> steps = new ArrayList<>();
        @Schema(description = "预计总时长（秒），无任何步骤时长时为 null")
        private Integer estimatedTotalSeconds;
    }

    /**
     * 待确认字段。
     *
     * @author XieMingJie
     * @since 2026-10-31 18:00
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DishTemplateAiUncertainFieldVo", description = "待确认字段")
    public static class UncertainFieldVo implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "字段路径，如 servings、ingredients[0].amount")
        private String path;

        @Schema(description = "中文名称，如「建议份数」")
        private String label;

        @Schema(description = "置信度：HIGH / MEDIUM / LOW")
        private String confidence;
    }
}
