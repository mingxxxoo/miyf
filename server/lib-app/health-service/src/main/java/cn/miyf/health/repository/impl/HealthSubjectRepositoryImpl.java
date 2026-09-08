package cn.miyf.health.repository.impl;

import cn.miyf.health.bean.entity.HealthSubjectEntity;
import cn.miyf.health.repository.HealthSubjectRepository;
import cn.miyf.health.repository.mapper.HealthSubjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 健康主体仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Repository
public class HealthSubjectRepositoryImpl implements HealthSubjectRepository {

    private final HealthSubjectMapper subjectMapper;

    /**
     * 构造仓储。
     *
     * @param subjectMapper Mapper
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSubjectRepositoryImpl(HealthSubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public long count() {
        Long n = subjectMapper.selectCount(Wrappers.<HealthSubjectEntity>lambdaQuery());
        return n == null ? 0L : n;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public List<HealthSubjectEntity> listByKeyword(String keyword) {
        return subjectMapper.selectList(Wrappers.<HealthSubjectEntity>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(HealthSubjectEntity::getDisplayName, keyword)
                        .or()
                        .like(HealthSubjectEntity::getRemark, keyword))
                .orderByDesc(HealthSubjectEntity::getLastModifyTime));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public Optional<HealthSubjectEntity> findById(Long id) {
        return Optional.ofNullable(subjectMapper.selectById(id));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void insert(HealthSubjectEntity entity) {
        subjectMapper.insert(entity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void updateById(HealthSubjectEntity entity) {
        subjectMapper.updateById(entity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void deleteById(Long id) {
        subjectMapper.deleteById(id);
    }
}
