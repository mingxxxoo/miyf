package cn.miyf.kitchen.bean.model;

import java.math.BigDecimal;

/**
 * 菜品有效评价聚合结果。
 *
 * @param avg   平均分
 * @param count 有效评价数
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
public record RatingAgg(BigDecimal avg, int count) {
}
