package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.entity.OperationLogEntity;
import cn.miyf.kitchen.bean.model.OperationLog;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.mapper.OperationLogMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * 操作日志仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Repository
public class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;

    /**
     * 构造仓储。
     *
     * @param operationLogMapper Mapper
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public OperationLogRepositoryImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public void save(OperationLog log) {
        OperationLogEntity entity = EntityConverters.toLogEntity(log);
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(Instant.now());
        }
        operationLogMapper.insert(entity);
        log.setId(entity.getId());
        log.setCreateTime(entity.getCreateTime());
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public PageResult<OperationLog> page(String operationType, String keyword, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        var records = operationLogMapper.selectPage(operationType, keyword, offset, pageSize).stream()
                .map(EntityConverters::toLog)
                .toList();
        long total = operationLogMapper.countPage(operationType, keyword);
        return PageResult.of(records, total, page, pageSize);
    }
}
