package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康数据源绑定数据访问接口（MyBatis Mapper）。
 * 按主体/状态查询与删除由 Service 使用 Wrapper 编排。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Mapper
public interface HealthProviderBindingRepository extends BaseMapper<HealthProviderBindingEntity> {
}
