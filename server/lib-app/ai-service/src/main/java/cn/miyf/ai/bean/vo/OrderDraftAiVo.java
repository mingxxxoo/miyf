package cn.miyf.ai.bean.vo;

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
 * 食客端 AI 预约草稿 VO（仅草稿，绝不自动下单）。
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
@Schema(name = "OrderDraftAiVo", description = "AI 预约草稿")
public class OrderDraftAiVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否降级")
    private boolean degraded;

    private List<ItemVo> items = new ArrayList<>();
    private String mealDate;
    private String mealType;
    private Integer guestCount;
    private String note;
    private List<UnmatchedVo> unmatched = new ArrayList<>();
    private String ambiguityNote;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class ItemVo {
        private String dishId;
        private String dishName;
        private Integer quantity;
        private String confidence;
        private String itemNote;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class UnmatchedVo {
        private String rawText;
        private String reason;
    }
}
