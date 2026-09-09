package cn.miyf.kitchen.bean.model;

/**
 * 名称维度计数（如热门菜品）。
 *
 * @param name  名称
 * @param count 数量
 * @author XieMingJie
 * @since 2026-09-05
 */
public record NameCount(String name, long count) {
}
