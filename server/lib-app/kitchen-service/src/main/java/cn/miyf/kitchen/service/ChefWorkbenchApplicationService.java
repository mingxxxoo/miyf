package cn.miyf.kitchen.service;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.ChefKitchenStatsVo;
import cn.miyf.kitchen.bean.vo.ChefWorkbenchSummaryVo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.KitchenBindingRepository;
import cn.miyf.kitchen.repository.OrderRepository;
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
 * 厨师工作台首页汇总与经营统计。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
@Service
@RequiredArgsConstructor
public class ChefWorkbenchApplicationService extends BaseApplicationService {

    private static final int RECENT_LIMIT = 5;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final KitchenAccessService kitchenAccessService;
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final KitchenBindingRepository bindingRepository;
    private final OrderApplicationService orderApplicationService;

    /**
     * 本厨工作台汇总。
     *
     * @return 角标计数 + 最新预约
     */
    public ChefWorkbenchSummaryVo summary() {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        Long kitchenId = kitchen.getId();

        OrderPageQo recentQo = new OrderPageQo();
        recentQo.setPage(1);
        recentQo.setRows(RECENT_LIMIT);
        PageResult<OrderVo> recentPage = orderApplicationService.pageChef(recentQo);
        List<OrderVo> recent = recentPage.records() != null
                ? recentPage.records()
                : List.of();

        return new ChefWorkbenchSummaryVo()
                .setKitchenId(kitchenId)
                .setKitchenName(kitchen.getName())
                .setKitchenStatus(kitchen.getStatus())
                .setPendingOrders(orderRepository.countChefPage(kitchenId, "PENDING"))
                .setPreparingOrders(orderRepository.countChefPage(kitchenId, "PREPARING"))
                .setReadyOrders(orderRepository.countChefPage(kitchenId, "READY"))
                .setOnSaleDishes(dishRepository.countChefPage(kitchenId, "ON_SALE", null, null))
                .setBoundDiners(bindingRepository.countChefPage(kitchenId, "BOUND"))
                .setPendingBindings(bindingRepository.countChefPage(kitchenId, "PENDING"))
                .setRecentOrders(recent);
    }

    /**
     * 本厨经营统计（近 7 日趋势 + 热门菜）。
     *
     * @return 统计 VO
     */
    public ChefKitchenStatsVo stats() {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        Long kitchenId = kitchen.getId();
        LocalDate today = LocalDate.now(ZONE);
        Instant todayStart = today.atStartOfDay(ZONE).toInstant();
        Instant weekStart = today.minusDays(6).atStartOfDay(ZONE).toInstant();

        List<String> last7Days = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            last7Days.add(today.minusDays(i).format(DAY_LABEL));
        }
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : orderRepository.countDailySinceByKitchen(kitchenId, weekStart)) {
            Object label = row.get("day_label");
            Object cnt = row.get("cnt");
            if (label != null && cnt instanceof Number number) {
                trendMap.put(String.valueOf(label), number.longValue());
            }
        }
        List<ChefKitchenStatsVo.TrendPoint> trend = new ArrayList<>();
        for (String day : last7Days) {
            trend.add(new ChefKitchenStatsVo.TrendPoint(day, trendMap.getOrDefault(day, 0L)));
        }

        List<ChefKitchenStatsVo.HotDishPoint> hot = new ArrayList<>();
        for (Map<String, Object> row : orderRepository.hotDishesSinceByKitchen(kitchenId, weekStart, 8)) {
            Object name = row.get("name");
            Object cnt = row.get("cnt");
            if (name != null && cnt instanceof Number number) {
                hot.add(new ChefKitchenStatsVo.HotDishPoint(String.valueOf(name), number.longValue()));
            }
        }

        return new ChefKitchenStatsVo()
                .setTodayOrders(orderRepository.countSinceByKitchen(kitchenId, todayStart))
                .setWeekOrders(orderRepository.countSinceByKitchen(kitchenId, weekStart))
                .setOnSaleDishes(dishRepository.countChefPage(kitchenId, "ON_SALE", null, null))
                .setBoundDiners(bindingRepository.countChefPage(kitchenId, "BOUND"))
                .setPendingOrders(orderRepository.countChefPage(kitchenId, "PENDING"))
                .setReservationTrend(trend)
                .setHotDishes(hot);
    }
}
