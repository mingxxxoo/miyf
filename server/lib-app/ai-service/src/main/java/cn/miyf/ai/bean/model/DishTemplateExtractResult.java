package cn.miyf.ai.bean.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 菜品模板抽取原始结果（对应 {@code DISH_TEMPLATE} 场景的 JSON Schema）。
 * <p>
 * 仅承载模型输出，不落库；字段缺失或不可判断时由模型填 null，
 * 后续由 {@code DishTemplateExtractNormalizer} 统一校验、裁剪并生成确认页 VO。
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
@Accessors(chain = true)
public class DishTemplateExtractResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜名。
     */
    private String name;

    /**
     * 菜品介绍。
     */
    private String description;

    /**
     * 建议份数。
     */
    private Integer servings;

    /**
     * 难度：EASY / MEDIUM / HARD。
     */
    private String difficulty;

    /**
     * 标签。
     */
    private List<String> tags = new ArrayList<>();

    /**
     * 原材料。
     */
    private List<IngredientDraft> ingredients = new ArrayList<>();

    /**
     * 做菜步骤。
     */
    private List<StepDraft> steps = new ArrayList<>();

    /**
     * 非菜品素材的拒绝原因。
     */
    private String rejectReason;

    /**
     * 整体置信度：HIGH / MEDIUM / LOW。
     */
    private String overallConfidence;

    /**
     * 不确定字段：点路径 → 置信度。
     */
    private Map<String, String> fieldConfidence = new LinkedHashMap<>();

    /**
     * 原材料草稿。
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
    public static class IngredientDraft implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String name;
        private String amount;
        private String unit;
        private String processing;
        private Boolean required;
        private String substitute;
        private String note;
    }

    /**
     * 步骤草稿。
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
    public static class StepDraft implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String description;
        private Integer durationSeconds;
        private String timerType;
        private String warning;
        private String completionHint;
        private Boolean required;
        private Boolean canUndo;
    }
}
