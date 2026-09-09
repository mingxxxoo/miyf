package cn.miyf.health.repository;

import cn.miyf.health.bean.entity.HealthSubjectEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康主体数据访问接口（MyBatis Mapper）。
 * 条件查询使用 MyBatis-Plus Wrapper，由 ApplicationService 组装。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Mapper
public interface HealthSubjectRepository extends BaseMapper<HealthSubjectEntity> {
}
