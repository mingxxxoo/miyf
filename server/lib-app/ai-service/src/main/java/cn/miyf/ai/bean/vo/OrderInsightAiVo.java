package cn.miyf.ai.bean.vo;

import cn.miyf.ai.bean.model.OrderInsightAiResult;
import cn.miyf.bean.vo.BaseVo;
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
 * 食客端饮食参考分析 VO。
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
@Schema(name = "OrderInsightAiVo", description = "AI 饮食参考")
public class OrderInsightAiVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否降级（分析暂不可用）")
    private boolean degraded;

    private String overallVerdict;
    private String summary;
    private List<DishInsightVo> dishInsights = new ArrayList<>();
    private List<SuggestionVo> suggestions = new ArrayList<>();
    private String disclaimer = OrderInsightAiResult.DEFAULT_DISCLAIMER;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class DishInsightVo {
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
    public static class SuggestionVo {
        private String type;
        private String text;
        private String replaceDishId;
        private String replaceDishName;
    }
}
