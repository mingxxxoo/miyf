package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.OperationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志数据访问接口（MyBatis Mapper）。
 * 管理端只读分页；写入由操作切面或业务编排调用 {@link BaseMapper#insert}。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OperationLogRepository extends BaseMapper<OperationLogEntity> {

    /**
     * 操作日志分页列表，可按操作类型与关键词筛选。
     *
     * @param operationType 操作类型，可空
     * @param keyword       关键词，可空
     * @param offset        偏移
     * @param limit         条数
     * @return 日志列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<OperationLogEntity> selectPage(@Param("operationType") String operationType,
                                        @Param("keyword") String keyword,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 操作日志分页总数。
     *
     * @param operationType 操作类型，可空
     * @param keyword       关键词，可空
     * @return 总数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countPage(@Param("operationType") String operationType,
                   @Param("keyword") String keyword);
}
