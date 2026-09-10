/**
 * OSS 文件服务：资源索引、分区存储、{@code /api/upload} 与 {@code GET /r/{fileId}}。
 * VO 字段 {@link cn.miyf.oss.security.FileAccess} 由响应拦截改写为签名 URL（HMAC 密钥
 * {@code app.file-storage.access-sign-secret}），并可选写入 Redis 临时访问权。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
package cn.miyf.oss;
