package cn.miyf.health.repository.impl;

import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.repository.HealthSampleRepository;
import cn.miyf.health.repository.mapper.HealthSampleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 健康采样仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Repository
public class HealthSampleRepositoryImpl implements HealthSampleRepository {

    private final HealthSampleMapper sampleMapper;

    /**
     * 构造仓储。
     *
     * @param sampleMapper Mapper
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSampleRepositoryImpl(HealthSampleMapper sampleMapper) {
        this.sampleMapper = sampleMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public long count() {
        Long n = sampleMapper.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery());
        return n == null ? 0L : n;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthSampleEntity> list(Long subjectId, String metricCode, int limit) {
        return sampleMapper.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(subjectId != null, HealthSampleEntity::getSubjectId, subjectId)
                .eq(StringUtils.hasText(metricCode), HealthSampleEntity::getMetricCode, metricCode)
                .orderByDesc(HealthSampleEntity::getMeasuredTime)
                .last("LIMIT " + limit));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthSampleEntity> listTrend(Long subjectId, String metricCode, Instant from, Instant to, int limit) {
        return sampleMapper.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(HealthSampleEntity::getSubjectId, subjectId)
                .eq(HealthSampleEntity::getMetricCode, metricCode)
                .ge(from != null, HealthSampleEntity::getMeasuredTime, from)
                .le(to != null, HealthSampleEntity::getMeasuredTime, to)
                .orderByAsc(HealthSampleEntity::getMeasuredTime)
                .last("LIMIT " + limit));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<HealthSampleEntity> findById(Long id) {
        return Optional.ofNullable(sampleMapper.selectById(id));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public boolean existsByProviderAndSourceSampleId(String providerCode, String sourceSampleId) {
        Long n = sampleMapper.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(HealthSampleEntity::getProviderCode, providerCode)
                .eq(HealthSampleEntity::getSourceSampleId, sourceSampleId));
        return n != null && n > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void insert(HealthSampleEntity entity) {
        sampleMapper.insert(entity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void deleteById(Long id) {
        sampleMapper.deleteById(id);
    }
}
