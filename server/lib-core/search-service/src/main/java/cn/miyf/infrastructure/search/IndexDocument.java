package cn.miyf.infrastructure.search;

/**
 * 待写入 Elasticsearch 的文档载体。
 * 用于 bulk 批量索引，id 为文档主键，document 为可 Jackson 序列化的业务对象。
 *
 * @param id       文档 ID
 * @param document 文档对象
 * @author XieMingJie
 * @since 2026-09-08
 */
public record IndexDocument(String id, Object document) {
}
