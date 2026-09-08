package cn.miyf.infrastructure.search;

import java.util.List;

/**
 * 分页搜索结果。
 * 同时带回请求时的 from/size，便于上层拼装分页响应。
 *
 * @param hits  命中列表
 * @param total 总命中数
 * @param from  起始偏移
 * @param size  页大小
 * @param <T>   文档类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public record SearchPage<T>(List<SearchHit<T>> hits, long total, int from, int size) {

    /**
     * 构造空分页结果（零命中）。
     *
     * @param from 偏移
     * @param size 页大小
     * @param <T>  文档类型
     * @return 空页
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static <T> SearchPage<T> empty(int from, int size) {
        return new SearchPage<>(List.of(), 0L, from, size);
    }
}
