package cn.miyf.kitchen.bean.model;

/**
 * 按日统计计数。
 *
 * @param date  日期标签（如 MM-dd）
 * @param count 数量
 * @author XieMingJie
 * @since 2026-09-05
 */
public record DailyCount(String date, long count) {
}
