package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSyncRunEntity;

import java.util.List;
import java.util.Optional;

/**
 * 健康同步运行记录仓储。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface HealthSyncRunRepository {

    /**
     * 同步运行记录列表。
     *
     * @param providerCode 数据源，可空
     * @param subjectId    主体，可空
     * @param limit        条数上限
     * @return 列表（按开始时间倒序）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthSyncRunEntity> list(String providerCode, Long subjectId, int limit);

    /**
     * 按 ID 查询。
     *
     * @param id ID
     * @return 记录
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<HealthSyncRunEntity> findById(Long id);

    /**
     * 插入。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void insert(HealthSyncRunEntity entity);

    /**
     * 按 ID 更新。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void updateById(HealthSyncRunEntity entity);
}
