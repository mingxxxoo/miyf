package cn.miyf.kitchen.service;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.OperationLogPageQo;
import cn.miyf.kitchen.bean.vo.OperationLogVo;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.OperationLogRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志应用服务（管理端只读）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
@RequiredArgsConstructor
public class OperationLogApplicationService extends BaseApplicationService {

    private final OperationLogRepository operationLogRepository;

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
        long off = offset(page, rows);
        var records = operationLogRepository.selectPage(qo.getOperationType(), qo.getKeyword(), off, rows).stream()
                .map(EntityConverters::toLog)
                .map(this::toVo)
                .toList();
        long total = operationLogRepository.countPage(qo.getOperationType(), qo.getKeyword());
        return PageResult.of(records, total, page, rows);
    }

    private OperationLogVo toVo(cn.miyf.kitchen.bean.model.OperationLog log) {
        return new OperationLogVo()
                .setId(log.getId())
                .setOperatorId(log.getOperatorId())
                .setOperatorName(log.getOperatorName())
                .setAction(log.getOperationType())
                .setOperationType(log.getOperationType())
                .setModule(log.getTargetType())
                .setDetail(log.getOperationDetail())
                .setIp(log.getRequestIp())
                .setRequestMethod(log.getRequestMethod())
                .setRequestUri(log.getRequestUri())
                .setCreateTime(log.getCreateTime());
    }
}
