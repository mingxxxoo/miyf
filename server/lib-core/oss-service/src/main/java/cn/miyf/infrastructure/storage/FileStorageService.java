package cn.miyf.infrastructure.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地实现 + 预留对象存储。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
public interface FileStorageService {

    /**
     * 存储文件流，由服务端生成对象键与公开 URL。
     *
     * @param inputStream  内容流（调用方负责关闭）
     * @param size         字节数
     * @param contentType  声明的 Content-Type
     * @param originalName 原始文件名（仅用于日志，不参与落盘名）
     * @return 存储结果
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    StoredFile store(InputStream inputStream, long size, String contentType, String originalName);

    /**
     * 删除已存储对象（可选实现）。
     *
     * @param objectKey 对象键
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    void delete(String objectKey);
}

