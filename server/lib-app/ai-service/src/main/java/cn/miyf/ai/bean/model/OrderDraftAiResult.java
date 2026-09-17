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
 * 食客端预约草稿结构化输出。
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
public class OrderDraftAiResult {

    private List<DraftItem> items = new ArrayList<>();
    private String mealDate;
    private String mealType;
    private Integer guestCount;
    private String note;
    private List<UnmatchedItem> unmatched = new ArrayList<>();
    private String ambiguityNote;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class DraftItem {
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
    public static class UnmatchedItem {
        private String rawText;
        private String reason;
    }
}
