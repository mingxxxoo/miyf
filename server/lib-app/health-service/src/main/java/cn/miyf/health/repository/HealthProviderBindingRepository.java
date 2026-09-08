package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthProviderBindingEntity;

import java.util.List;
import java.util.Optional;

/**
 * 健康数据源绑定仓储。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface HealthProviderBindingRepository {

    /**
     * 按主体列出绑定。
     *
     * @param subjectId 主体
     * @return 列表（按 providerCode 升序）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthProviderBindingEntity> listBySubjectId(Long subjectId);

    /**
     * 按状态列出绑定。
     *
     * @param status 状态
     * @return 列表（providerCode、subjectId 升序）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthProviderBindingEntity> listByStatus(String status);

    /**
     * 按主体与数据源查询。
     *
     * @param subjectId    主体
     * @param providerCode 数据源
     * @return 绑定
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<HealthProviderBindingEntity> findBySubjectAndProvider(Long subjectId, String providerCode);

    /**
     * 按主体、数据源与状态查询。
     *
     * @param subjectId    主体
     * @param providerCode 数据源
     * @param status       状态
     * @return 绑定
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<HealthProviderBindingEntity> findBySubjectProviderAndStatus(Long subjectId, String providerCode,
                                                                         String status);

    /**
     * 删除主体下全部绑定。
     *
     * @param subjectId 主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void deleteBySubjectId(Long subjectId);

    /**
     * 插入。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void insert(HealthProviderBindingEntity entity);

    /**
     * 按 ID 更新。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void updateById(HealthProviderBindingEntity entity);
}
