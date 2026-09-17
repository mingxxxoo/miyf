package cn.miyf.ai.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 厨师端 AI 建菜确认页 VO（不落库，由厨师确认后走现有建菜接口）。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChefDishAiExtractVo", description = "AI 建菜抽取结果")
public class ChefDishAiExtractVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否降级（AI 不可用）")
    private boolean degraded;

    @Schema(description = "降级/拒绝原因")
    private String rejectReason;

    @Schema(description = "菜品草稿列表")
    private List<DishDraftVo> dishes = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "ChefDishAiDishDraftVo", description = "单道菜草稿")
    public static class DishDraftVo {
        private String name;
        private String subtitle;
        private String categoryName;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "匹配到的已有分类 ID", type = "string")
        private Long categoryId;
        @Schema(description = "是否需新建分类")
        private boolean newCategory;
        private String description;
        private List<String> tags = new ArrayList<>();
        private String stockType;
        private Integer stock;
        private RecipeDraftVo recipe;
        private Map<String, Object> nutrition;
        @Schema(description = "服务端截断标记")
        private boolean aiTruncated;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class RecipeDraftVo {
        private List<NamedAmountVo> ingredients = new ArrayList<>();
        private List<NamedAmountVo> seasonings = new ArrayList<>();
        private List<StepDraftVo> steps = new ArrayList<>();
        private String difficulty;
        private Integer prepareMinutes;
        private Integer cookMinutes;
        private Integer servings;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class NamedAmountVo {
        private String name;
        private String amount;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class StepDraftVo {
        private Integer step;
        private String content;
        private Integer durationMinutes;
    }
}
