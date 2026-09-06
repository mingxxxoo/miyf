package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.repository.BaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 预约单仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface OrderRepository extends BaseRepository<Order, Long> {

    /**
     * 插入预约头并写入明细。
     *
     * @param order 含明细的预约单
     * @return 保存后的预约单
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    Order insertWithItems(Order order);

    /**
     * 乐观更新预约状态：仅当当前为 fromStatus 时成功。
     *
     * @param id         预约 ID
     * @param fromStatus 当前状态
     * @param toStatus   目标状态
     * @return true 更新成功
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    boolean updateStatus(Long id, String fromStatus, String toStatus);

    /**
     * 用户端分页（仅本人）。
     *
     * @param userId   用户 ID
     * @param status   状态，可空
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<Order> pageByUser(Long userId, String status, long page, long pageSize);

    /**
     * 管理端分页。
     *
     * @param status   状态，可空
     * @param orderNo  预约号，可空
     * @param userId   用户 ID，可空
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<Order> pageAdmin(String status, String orderNo, Long userId, long page, long pageSize);

    /**
     * 统计指定状态且创建时间不早于 since 的预约数。
     *
     * @param status 状态
     * @param since  起始时间，可空
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countByStatusSince(String status, Instant since);

    /**
     * 统计创建时间不早于 since 的预约数。
     *
     * @param since 起始时间，可空
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countSince(Instant since);

    /**
     * 按状态分组统计。
     *
     * @return 状态 → 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    Map<String, Long> countGroupByStatus();

    /**
     * 近 N 日预约趋势（按日）。
     *
     * @param since 起始时间
     * @return dateLabel → count（已按日排序）
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<DailyCount> countDailySince(Instant since);

    /**
     * 近 N 日活跃用户（按下单用户去重）。
     *
     * @param since 起始时间
     * @return dateLabel → activeUsers
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<DailyCount> countActiveUsersDailySince(Instant since);

    /**
     * 热门菜品 TOP。
     *
     * @param since 起始时间
     * @param limit 条数
     * @return 菜品名 → 份数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<NameCount> hotDishesSince(Instant since, int limit);

    /**
     * 按日计数值。
     *
     * @param date  展示日期（MM-DD）
     * @param count 数量
     */
    record DailyCount(String date, long count) {
    }

    /**
     * 名称计数值。
     *
     * @param name  名称
     * @param count 数量
     */
    record NameCount(String name, long count) {
    }
}
