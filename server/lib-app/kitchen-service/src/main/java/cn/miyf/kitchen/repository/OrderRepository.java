package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 预约单数据访问接口（MyBatis Mapper）。
 * 单表 CRUD 继承 {@link BaseMapper}；分页/统计/乐观状态更新见 OrderRepository.xml。
 * 明细装载与「头+明细」写入由 OrderApplicationService 编排。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OrderRepository extends BaseMapper<OrderEntity> {

    /**
     * 乐观更新预约状态：仅当当前状态为 fromStatus 时成功。
     * 影响行数为 0 表示并发下状态已变更，调用方应视为冲突。
     *
     * @param id         预约 ID
     * @param fromStatus 期望原状态
     * @param toStatus   目标状态
     * @return 影响行数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") String fromStatus,
                     @Param("status") String toStatus);

    /**
     * 用户端分页：仅本人预约，可按状态筛选。
     *
     * @param userId 用户 ID
     * @param status 状态，可空
     * @param offset 偏移
     * @param limit  条数
     * @return 预约列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<OrderEntity> selectUserPage(@Param("userId") Long userId,
                                     @Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /**
     * 用户端分页总数。
     *
     * @param userId 用户 ID
     * @param status 状态，可空
     * @return 总数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countUserPage(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 管理端分页，可按状态、预约号、用户筛选。
     *
     * @param status  状态，可空
     * @param orderNo 预约号，可空
     * @param userId  用户 ID，可空
     * @param offset  偏移
     * @param limit   条数
     * @return 预约列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<OrderEntity> selectAdminPage(@Param("status") String status,
                                      @Param("orderNo") String orderNo,
                                      @Param("userId") Long userId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    /**
     * 管理端分页总数。
     *
     * @param status  状态，可空
     * @param orderNo 预约号，可空
     * @param userId  用户 ID，可空
     * @return 总数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countAdminPage(@Param("status") String status,
                        @Param("orderNo") String orderNo,
                        @Param("userId") Long userId);

    /**
     * 统计指定状态且创建时间不早于 since 的预约数。
     *
     * @param status 状态
     * @param since  起始时间，可空
     * @return 数量
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countByStatusSince(@Param("status") String status, @Param("since") Instant since);

    /**
     * 统计创建时间不早于 since 的预约数。
     *
     * @param since 起始时间，可空
     * @return 数量
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countSince(@Param("since") Instant since);

    /**
     * 按状态分组统计，每行含 status、cnt。
     *
     * @return 分组行列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<Map<String, Object>> countGroupByStatus();

    /**
     * 按日统计预约量（自 since 起），每行含 day_key、day_label、cnt。
     *
     * @param since 起始时间
     * @return 按日统计行
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> countDailySince(@Param("since") Instant since);

    /**
     * 按日统计活跃用户数（按下单用户去重，自 since 起）。
     *
     * @param since 起始时间
     * @return 按日统计行
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> countActiveUsersDailySince(@Param("since") Instant since);

    /**
     * 热门菜品：自 since 起按预约份数汇总，每行含 name、cnt。
     *
     * @param since 起始时间
     * @param limit 条数
     * @return 菜品名与份数行
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> hotDishesSince(@Param("since") Instant since, @Param("limit") int limit);
}
