package cn.miyf.infrastructure.search;

/**
 * 单条搜索命中结果。
 * score 在部分排序场景可能为 {@link Float#NaN}，调用方需按业务容忍处理。
 *
 * @param id     文档 ID
 * @param score  相关性分数
 * @param source 反序列化后的文档正文
 * @param <T>    文档类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public record SearchHit<T>(String id, float score, T source) {
}
