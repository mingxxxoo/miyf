package cn.miyf.kitchen.service;

import cn.miyf.kitchen.bean.model.DailyCount;
import cn.miyf.kitchen.bean.model.NameCount;
import cn.miyf.kitchen.bean.model.RatingCount;
import cn.miyf.kitchen.bean.vo.DashboardStatsVo;
import cn.miyf.kitchen.repository.CommentRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DashboardApplicationService extends BaseApplicationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final CommentRepository commentRepository;

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

        Map<String, Long> trendMap = toCountMap(mapDaily(orderRepository.countDailySince(weekStart)));
        Map<String, Long> activityMap = toCountMap(mapDaily(orderRepository.countActiveUsersDailySince(weekStart)));

        List<DashboardStatsVo.TrendPoint> trend = new ArrayList<>();
        List<DashboardStatsVo.ActivityPoint> activity = new ArrayList<>();
        for (String day : last7Days) {
            trend.add(new DashboardStatsVo.TrendPoint(day, trendMap.getOrDefault(day, 0L)));
            activity.add(new DashboardStatsVo.ActivityPoint(day, activityMap.getOrDefault(day, 0L)));
        }

        List<DashboardStatsVo.HotDishPoint> hotDishes = mapNameCount(orderRepository.hotDishesSince(weekStart, 8)).stream()
                .map(h -> new DashboardStatsVo.HotDishPoint(h.name(), h.count()))
                .toList();

        List<DashboardStatsVo.RatingPoint> ratings = ratingDistribution().stream()
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

    private List<RatingCount> ratingDistribution() {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : commentRepository.countGroupByRating()) {
            Object rating = row.get("rating");
            Object cnt = row.get("cnt");
            if (rating instanceof Number r && cnt instanceof Number c) {
                map.put(r.intValue(), c.longValue());
            }
        }
        List<RatingCount> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            list.add(new RatingCount(i, map.getOrDefault(i, 0L)));
        }
        return list;
    }

    private static List<DailyCount> mapDaily(List<Map<String, Object>> rows) {
        List<DailyCount> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object label = row.get("day_label");
            Object cnt = row.get("cnt");
            if (label != null && cnt instanceof Number number) {
                list.add(new DailyCount(String.valueOf(label), number.longValue()));
            }
        }
        return list;
    }

    private static List<NameCount> mapNameCount(List<Map<String, Object>> rows) {
        List<NameCount> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object name = row.get("name");
            Object cnt = row.get("cnt");
            if (name != null && cnt instanceof Number number) {
                list.add(new NameCount(String.valueOf(name), number.longValue()));
            }
        }
        return list;
    }

    private static Map<String, Long> toCountMap(List<DailyCount> rows) {
        Map<String, Long> map = new HashMap<>();
        for (DailyCount row : rows) {
            map.put(row.date(), row.count());
        }
        return map;
    }
}
