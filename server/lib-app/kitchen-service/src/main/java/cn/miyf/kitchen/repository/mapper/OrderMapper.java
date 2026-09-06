package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 预约单 Mapper；复杂查询见 OrderMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    /**
     * 乐观更新状态：仅当当前状态为 fromStatus 时更新。
     *
     * @param id         预约 ID
     * @param fromStatus 期望原状态
     * @param toStatus   目标状态
     * @return 影响行数，0 表示状态已变更
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") String fromStatus,
                     @Param("status") String toStatus);

    /**
     * 用户端分页。
     *
     * @param userId 用户 ID
     * @param status 状态
     * @param offset 偏移
     * @param limit  条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<OrderEntity> selectUserPage(@Param("userId") Long userId,
                                     @Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /**
     * 用户端分页总数。
     *
     * @param userId 用户 ID
     * @param status 状态
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countUserPage(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 管理端分页。
     *
     * @param status  状态
     * @param orderNo 预约号
     * @param userId  用户 ID
     * @param offset  偏移
     * @param limit   条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<OrderEntity> selectAdminPage(@Param("status") String status,
                                      @Param("orderNo") String orderNo,
                                      @Param("userId") Long userId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    /**
     * 管理端分页总数。
     *
     * @param status  状态
     * @param orderNo 预约号
     * @param userId  用户 ID
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countAdminPage(@Param("status") String status,
                        @Param("orderNo") String orderNo,
                        @Param("userId") Long userId);

    /**
     * 按状态与起始时间统计。
     *
     * @param status 状态
     * @param since  起始时间
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countByStatusSince(@Param("status") String status, @Param("since") Instant since);

    /**
     * 按起始时间统计。
     *
     * @param since 起始时间
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countSince(@Param("since") Instant since);

    /**
     * 按状态分组统计。
     *
     * @return 每行含 status、cnt
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<Map<String, Object>> countGroupByStatus();

    /**
     * 按日统计预约量（自 since 起）。
     *
     * @param since 起始时间
     * @return 每行含 day_key、day_label、cnt
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> countDailySince(@Param("since") Instant since);

    /**
     * 按日统计活跃用户数（自 since 起）。
     *
     * @param since 起始时间
     * @return 每行含 day_key、day_label、cnt
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> countActiveUsersDailySince(@Param("since") Instant since);

    /**
     * 热门菜品（自 since 起按份数汇总）。
     *
     * @param since 起始时间
     * @param limit 条数
     * @return 每行含 name、cnt
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> hotDishesSince(@Param("since") Instant since, @Param("limit") int limit);
}
