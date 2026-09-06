package cn.miyf.common;

import java.util.List;

/**
 * 统一分页结果。
 *
 * @param records  当前页记录
 * @param total    总记录数
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 * @param <T>      记录类型
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public record PageResult<T>(List<T> records, long total, long page, long pageSize) {

    /**
     * 构造分页结果。
     *
     * @param records  记录列表
     * @param total    总数
     * @param page     页码
     * @param pageSize 每页条数
     * @param <T>      类型
     * @return 分页对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
