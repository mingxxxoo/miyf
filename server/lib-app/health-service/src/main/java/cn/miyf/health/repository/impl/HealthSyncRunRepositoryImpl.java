package cn.miyf.health.repository.impl;

import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.repository.HealthSyncRunRepository;
import cn.miyf.health.repository.mapper.HealthSyncRunMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 健康同步运行记录仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Repository
public class HealthSyncRunRepositoryImpl implements HealthSyncRunRepository {

    private final HealthSyncRunMapper syncRunMapper;

    /**
     * 构造仓储。
     *
     * @param syncRunMapper Mapper
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSyncRunRepositoryImpl(HealthSyncRunMapper syncRunMapper) {
        this.syncRunMapper = syncRunMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthSyncRunEntity> list(String providerCode, Long subjectId, int limit) {
        return syncRunMapper.selectList(Wrappers.<HealthSyncRunEntity>lambdaQuery()
                .eq(StringUtils.hasText(providerCode), HealthSyncRunEntity::getProviderCode, providerCode)
                .eq(subjectId != null, HealthSyncRunEntity::getSubjectId, subjectId)
                .orderByDesc(HealthSyncRunEntity::getStartedTime)
                .last("LIMIT " + limit));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<HealthSyncRunEntity> findById(Long id) {
        return Optional.ofNullable(syncRunMapper.selectById(id));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void insert(HealthSyncRunEntity entity) {
        syncRunMapper.insert(entity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void updateById(HealthSyncRunEntity entity) {
        syncRunMapper.updateById(entity);
    }
}
