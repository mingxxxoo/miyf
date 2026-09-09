package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康同步运行记录数据访问接口（MyBatis Mapper）。
 * 同步任务列表筛选由 Service 通过 Wrapper 完成。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Mapper
public interface HealthSyncRunRepository extends BaseMapper<HealthSyncRunEntity> {
}
