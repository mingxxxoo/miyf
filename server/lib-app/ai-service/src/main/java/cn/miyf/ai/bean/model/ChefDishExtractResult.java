package cn.miyf.ai.bean.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 厨师端菜品抽取结构化输出。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChefDishExtractResult {

    private List<DishDraft> dishes = new ArrayList<>();
    private String rejectReason;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class DishDraft {
        private String name;
        private String subtitle;
        private String categoryName;
        private String description;
        private List<String> tags = new ArrayList<>();
        private String stockType;
        private Integer stock;
        private RecipeDraft recipe;
        private Map<String, Object> nutrition;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class RecipeDraft {
        private List<NamedAmount> ingredients = new ArrayList<>();
        private List<NamedAmount> seasonings = new ArrayList<>();
        private List<StepDraft> steps = new ArrayList<>();
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
    public static class NamedAmount {
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
    public static class StepDraft {
        private Integer step;
        private String content;
        private Integer durationMinutes;
    }
}
