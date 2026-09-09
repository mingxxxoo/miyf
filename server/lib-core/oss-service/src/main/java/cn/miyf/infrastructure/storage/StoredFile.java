package cn.miyf.infrastructure.storage;

/**
 * 存储成功结果。
 *
 * @param path        相对存储路径（无后缀，以文件 ID 为名）
 * @param contentType 规范化 MIME
 * @param size        字节数
 * @param md5         内容 MD5（小写 hex）
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
public record StoredFile(String path, String contentType, long size, String md5) {
}
