package cn.miyf.kitchen.service;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.OperationLog;
import cn.miyf.kitchen.bean.qo.OperationLogPageQo;
import cn.miyf.kitchen.bean.vo.OperationLogVo;
import cn.miyf.kitchen.repository.OperationLogRepository;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;

/**
 * 操作日志应用服务（管理端只读）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
public class OperationLogApplicationService extends BaseApplicationService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 构造服务。
     *
     * @param operationLogRepository 日志仓储
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public OperationLogApplicationService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    /**
     * 管理端分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public PageResult<OperationLogVo> pageAdmin(OperationLogPageQo qo) {
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        PageResult<OperationLog> result = operationLogRepository.page(
                qo.getOperationType(), qo.getKeyword(), page, rows);
        return PageResult.of(result.records().stream().map(this::toVo).toList(),
                result.total(), result.page(), result.pageSize());
    }

    private OperationLogVo toVo(OperationLog log) {
        return new OperationLogVo()
                .setId(log.getId())
                .setOperatorId(log.getOperatorId())
                .setOperatorName(log.getOperatorName())
                .setAction(log.getOperationType())
                .setModule(log.getTargetType())
                .setDetail(log.getOperationDetail())
                .setIp(log.getRequestIp())
                .setCreatedAt(log.getCreatedAt());
    }
}
