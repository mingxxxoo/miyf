package cn.miyf.health.repository.mapper;

import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康同步任务 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Mapper
public interface HealthSyncRunMapper extends BaseMapper<HealthSyncRunEntity> {
}
