package cn.miyf.infrastructure.storage;

/**
 * 存储成功结果。
 *
 * @param objectKey   相对对象键，如 2026/09/05/Long.jpg
 * @param url         可访问 URL
 * @param contentType 规范化 MIME
 * @param size        字节数
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
public record StoredFile(String objectKey, String url, String contentType, long size) {
}
