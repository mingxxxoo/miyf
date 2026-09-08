package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSubjectEntity;

import java.util.List;
import java.util.Optional;

/**
 * 健康主体仓储。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface HealthSubjectRepository {

    /**
     * 主体总数。
     *
     * @return 数量
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    long count();

    /**
     * 按关键字检索主体（展示名 / 备注）。
     *
     * @param keyword 关键字，可空
     * @return 列表（按最近修改倒序）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthSubjectEntity> listByKeyword(String keyword);

    /**
     * 按 ID 查询。
     *
     * @param id ID
     * @return 主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<HealthSubjectEntity> findById(Long id);

    /**
     * 插入。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void insert(HealthSubjectEntity entity);

    /**
     * 按 ID 更新。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void updateById(HealthSubjectEntity entity);

    /**
     * 按 ID 删除。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void deleteById(Long id);
}
