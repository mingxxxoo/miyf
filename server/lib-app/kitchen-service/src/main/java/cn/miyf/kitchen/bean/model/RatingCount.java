package cn.miyf.kitchen.bean.model;

/**
 * 星级分布计数。
 *
 * @param rating 星级 1~5
 * @param count  数量
 * @author XieMingJie
 * @since 2026-09-05
 */
public record RatingCount(int rating, long count) {
}
