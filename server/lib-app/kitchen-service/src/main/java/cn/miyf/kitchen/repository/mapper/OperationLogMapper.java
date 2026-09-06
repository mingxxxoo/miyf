package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.OperationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

    /**
     * 分页查询。
     *
     * @param operationType 操作类型
     * @param keyword       关键词
     * @param offset        偏移
     * @param limit         条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<OperationLogEntity> selectPage(@Param("operationType") String operationType,
                                        @Param("keyword") String keyword,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 分页总数。
     *
     * @param operationType 操作类型
     * @param keyword       关键词
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countPage(@Param("operationType") String operationType,
                   @Param("keyword") String keyword);
}
