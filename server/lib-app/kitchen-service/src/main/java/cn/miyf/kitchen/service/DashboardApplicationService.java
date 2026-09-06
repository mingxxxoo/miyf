package cn.miyf.kitchen.service;

import cn.miyf.kitchen.bean.vo.DashboardStatsVo;
import cn.miyf.kitchen.repository.CommentRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端仪表盘统计服务。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
public class DashboardApplicationService extends BaseApplicationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final CommentRepository commentRepository;

    /**
     * 构造服务。
     *
     * @param userRepository    用户仓储
     * @param dishRepository    菜品仓储
     * @param orderRepository   预约仓储
     * @param commentRepository 评价仓储
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public DashboardApplicationService(UserRepository userRepository,
                                       DishRepository dishRepository,
                                       OrderRepository orderRepository,
                                       CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 聚合仪表盘统计。
     *
     * @return 统计 VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public DashboardStatsVo stats() {
        LocalDate today = LocalDate.now(ZONE);
        Instant todayStart = today.atStartOfDay(ZONE).toInstant();
        Instant weekStart = today.minusDays(6).atStartOfDay(ZONE).toInstant();

        List<String> last7Days = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            last7Days.add(today.minusDays(i).format(DAY_LABEL));
        }

        Map<String, Long> trendMap = toCountMap(orderRepository.countDailySince(weekStart));
        Map<String, Long> activityMap = toCountMap(orderRepository.countActiveUsersDailySince(weekStart));

        List<DashboardStatsVo.TrendPoint> trend = new ArrayList<>();
        List<DashboardStatsVo.ActivityPoint> activity = new ArrayList<>();
        for (String day : last7Days) {
            trend.add(new DashboardStatsVo.TrendPoint(day, trendMap.getOrDefault(day, 0L)));
            activity.add(new DashboardStatsVo.ActivityPoint(day, activityMap.getOrDefault(day, 0L)));
        }

        List<DashboardStatsVo.HotDishPoint> hotDishes = orderRepository.hotDishesSince(weekStart, 8).stream()
                .map(h -> new DashboardStatsVo.HotDishPoint(h.name(), h.count()))
                .toList();

        List<DashboardStatsVo.RatingPoint> ratings = commentRepository.ratingDistribution().stream()
                .map(r -> new DashboardStatsVo.RatingPoint(r.rating(), r.count()))
                .toList();

        return new DashboardStatsVo()
                .setUserCount(userRepository.countActive())
                .setTodayOrders(orderRepository.countSince(todayStart))
                .setDishCount(dishRepository.countOnSale())
                .setPendingComments(commentRepository.countHidden())
                .setReservationTrend(trend)
                .setHotDishes(new ArrayList<>(hotDishes))
                .setRatingDistribution(new ArrayList<>(ratings))
                .setUserActivity(activity);
    }

    private static Map<String, Long> toCountMap(List<OrderRepository.DailyCount> rows) {
        Map<String, Long> map = new HashMap<>();
        for (OrderRepository.DailyCount row : rows) {
            map.put(row.date(), row.count());
        }
        return map;
    }
}
