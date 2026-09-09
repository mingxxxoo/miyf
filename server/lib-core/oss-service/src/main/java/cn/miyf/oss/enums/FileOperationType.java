package cn.miyf.oss.enums;

/**
 * 文件操作类型。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public enum FileOperationType {

    /** 在线预览 / 读取。 */
    READ,

    /** 下载。 */
    DOWNLOAD,

    /** 上传。 */
    UPLOAD,

    /** 更新内容或元数据。 */
    UPDATE,

    /** 删除。 */
    DELETE,

    /** 确认临时文件（转为正式）。 */
    CONFIRM
}
