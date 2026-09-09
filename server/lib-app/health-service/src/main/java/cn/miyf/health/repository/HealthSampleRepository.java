package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSampleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康采样数据访问接口（MyBatis Mapper）。
 * 列表/趋势/幂等校验等条件查询由 Service 通过 Wrapper 完成。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Mapper
public interface HealthSampleRepository extends BaseMapper<HealthSampleEntity> {
}
