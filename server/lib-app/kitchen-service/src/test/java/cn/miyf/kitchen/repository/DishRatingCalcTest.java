package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.model.RatingAgg;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 评分聚合口径单测（与仓储聚合规则对齐）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
class DishRatingCalcTest {

    @Test
    void averageRoundedToTwoScale() {
        BigDecimal sum = BigDecimal.valueOf(5 + 4 + 5 + 3 + 5);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
        RatingAgg agg = new RatingAgg(avg, 5);
        assertEquals(new BigDecimal("4.40"), agg.avg());
        assertEquals(5, agg.count());
    }

    @Test
    void emptyCommentsUseZero() {
        RatingAgg agg = new RatingAgg(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
        assertEquals(0, agg.count());
        assertEquals(new BigDecimal("0.00"), agg.avg());
    }
}
