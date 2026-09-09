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
     * 按给定相对路径存储文件（路径已含文件 ID，无扩展名）。
     *
     * @param inputStream  内容流（调用方负责关闭）
     * @param size         字节数
     * @param contentType  声明的 Content-Type
     * @param path         相对存储路径
     * @return 存储结果（含 MD5）
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    StoredFile store(InputStream inputStream, long size, String contentType, String path);

    /**
     * 打开已存储对象读取流（调用方负责关闭）。
     *
     * @param path 相对存储路径
     * @return 输入流
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    InputStream open(String path);

    /**
     * 删除已存储对象。
     *
     * @param path 相对存储路径
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    void delete(String path);
}
