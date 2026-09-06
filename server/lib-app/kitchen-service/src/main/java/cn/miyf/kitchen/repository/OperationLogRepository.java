package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.OperationLog;

/**
 * 操作日志仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface OperationLogRepository {

    /**
     * 写入操作日志。
     *
     * @param log 日志
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    void save(OperationLog log);

    /**
     * 分页查询操作日志。
     *
     * @param operationType 操作类型，可空
     * @param keyword       关键词，可空
     * @param page          页码
     * @param pageSize      每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<OperationLog> page(String operationType, String keyword, long page, long pageSize);
}
