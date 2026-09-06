package cn.miyf.job.repository.mapper;

import cn.miyf.job.entity.SysJobStateEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务状态 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Mapper
public interface SysJobStateMapper extends BaseMapper<SysJobStateEntity> {
}
