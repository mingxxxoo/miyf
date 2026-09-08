package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSampleEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 健康采样仓储。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface HealthSampleRepository {

    /**
     * 采样总数。
     *
     * @return 数量
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    long count();

    /**
     * 采样列表（测量时间倒序）。
     *
     * @param subjectId  主体，可空
     * @param metricCode 指标，可空
     * @param limit      条数上限
     * @return 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthSampleEntity> list(Long subjectId, String metricCode, int limit);

    /**
     * 趋势序列（测量时间升序）。
     *
     * @param subjectId  主体
     * @param metricCode 指标
     * @param from       起始，可空
     * @param to         结束，可空
     * @param limit      点数上限
     * @return 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthSampleEntity> listTrend(Long subjectId, String metricCode, Instant from, Instant to, int limit);

    /**
     * 按 ID 查询。
     *
     * @param id ID
     * @return 采样
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Optional<HealthSampleEntity> findById(Long id);

    /**
     * 是否存在同源采样（幂等键）。
     *
     * @param providerCode   数据源
     * @param sourceSampleId 源采样 ID
     * @return true 已存在
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    boolean existsByProviderAndSourceSampleId(String providerCode, String sourceSampleId);

    /**
     * 插入。
     *
     * @param entity 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void insert(HealthSampleEntity entity);

    /**
     * 按 ID 删除。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    void deleteById(Long id);
}
