package cn.miyf.ai.repository;

import cn.miyf.ai.bean.entity.AiCallLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 调用日志 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Mapper
public interface AiCallLogRepository extends BaseMapper<AiCallLogEntity> {
}
