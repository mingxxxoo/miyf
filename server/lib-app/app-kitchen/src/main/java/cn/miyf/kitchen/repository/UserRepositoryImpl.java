package cn.miyf.kitchen.repository;

import cn.miyf.repository.AbstractMybatisRepository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.User;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.repository.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 用户仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Repository
public class UserRepositoryImpl extends AbstractMybatisRepository<User, UserEntity> implements UserRepository {

    private final UserMapper userMapper;

    /**
     * 构造用户仓储。
     *
     * @param userMapper 用户 Mapper
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public UserRepositoryImpl(UserMapper userMapper) {
        super(userMapper, EntityConverters::toUser, EntityConverters::toUserEntity);
        this.userMapper = userMapper;
    }

    @Override
    protected Long getDomainId(User domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(User domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(User domain, Instant createdAt, Instant updatedAt) {
        domain.setCreatedAt(createdAt);
        domain.setUpdatedAt(updatedAt);
    }

    /**
     * 新用户默认 ENABLED。
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    protected void beforeInsert(UserEntity entity, User domain) {
        if (entity.getStatus() == null) {
            entity.setStatus("ENABLED");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public Optional<User> findByOpenid(String openid) {
        return Optional.ofNullable(EntityConverters.toUser(userMapper.selectByOpenid(openid)));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public PageResult<User> page(String keyword, String status, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        var records = userMapper.selectAdminPage(keyword, status, offset, pageSize).stream()
                .map(EntityConverters::toUser).toList();
        long total = userMapper.countAdminPage(keyword, status);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public void updateStatus(Long id, String status) {
        userMapper.updateStatus(id, status);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public long countActive() {
        return userMapper.countActive();
    }
}
