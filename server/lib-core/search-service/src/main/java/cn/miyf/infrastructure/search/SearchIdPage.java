package cn.miyf.infrastructure.search;

import java.util.List;

/**
 * 仅含文档 ID 的分页结果，供业务「ES 召回 + 数据库回表」。
 *
 * @param ids      命中 ID（保持 ES 排序）
 * @param total    总命中数
 * @param page     页码（从 1 开始，与 AbstractCondition 一致）
 * @param pageSize 每页条数
 * @author XieMingJie
 * @since 2026-09-08
 */
public record SearchIdPage(List<String> ids, long total, int page, int pageSize) {

    /**
     * 空页。
     *
     * @param page     页码
     * @param pageSize 页大小
     * @return 空结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static SearchIdPage empty(int page, int pageSize) {
        return new SearchIdPage(List.of(), 0L, page, pageSize);
    }
}
