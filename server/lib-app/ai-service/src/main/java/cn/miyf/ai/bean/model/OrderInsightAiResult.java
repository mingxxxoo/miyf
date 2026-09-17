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

/**
 * 健康指标满足度分析结构化输出。
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
public class OrderInsightAiResult {

    public static final String DEFAULT_DISCLAIMER = "以上为一般性饮食参考，不构成医疗建议。如有疾病管理需求请遵医嘱。";

    private String overallVerdict;
    private String summary;
    private List<DishInsight> dishInsights = new ArrayList<>();
    private List<Suggestion> suggestions = new ArrayList<>();
    private String disclaimer = DEFAULT_DISCLAIMER;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class DishInsight {
        private String dishId;
        private String dishName;
        private String verdict;
        private String reason;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Suggestion {
        private String type;
        private String text;
        private String replaceDishId;
        private String replaceDishName;
    }
}
