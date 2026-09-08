package cn.miyf.health.repository.impl;

import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.repository.HealthProviderBindingRepository;
import cn.miyf.health.repository.mapper.HealthProviderBindingMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 健康数据源绑定仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Repository
public class HealthProviderBindingRepositoryImpl implements HealthProviderBindingRepository {

    private final HealthProviderBindingMapper bindingMapper;

    /**
     * 构造仓储。
     *
     * @param bindingMapper Mapper
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthProviderBindingRepositoryImpl(HealthProviderBindingMapper bindingMapper) {
        this.bindingMapper = bindingMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthProviderBindingEntity> listBySubjectId(Long subjectId) {
        return bindingMapper.selectList(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .orderByAsc(HealthProviderBindingEntity::getProviderCode));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthProviderBindingEntity> listByStatus(String status) {
        return bindingMapper.selectList(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getStatus, status)
                .orderByAsc(HealthProviderBindingEntity::getProviderCode)
                .orderByAsc(HealthProviderBindingEntity::getSubjectId));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<HealthProviderBindingEntity> findBySubjectAndProvider(Long subjectId, String providerCode) {
        return Optional.ofNullable(bindingMapper.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .last("LIMIT 1")));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<HealthProviderBindingEntity> findBySubjectProviderAndStatus(Long subjectId, String providerCode,
                                                                                String status) {
        return Optional.ofNullable(bindingMapper.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .eq(HealthProviderBindingEntity::getStatus, status)
                .last("LIMIT 1")));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void deleteBySubjectId(Long subjectId) {
        bindingMapper.delete(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void insert(HealthProviderBindingEntity entity) {
        bindingMapper.insert(entity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void updateById(HealthProviderBindingEntity entity) {
        bindingMapper.updateById(entity);
    }
}
