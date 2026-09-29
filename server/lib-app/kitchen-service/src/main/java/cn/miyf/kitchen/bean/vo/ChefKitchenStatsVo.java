package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 本厨经营统计（近 7 日趋势 + 热门菜）。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChefKitchenStatsVo", description = "厨师经营统计")
public class ChefKitchenStatsVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "今日预约数")
    private long todayOrders;

    @Schema(description = "近 7 日预约数")
    private long weekOrders;

    @Schema(description = "在售菜品数")
    private long onSaleDishes;

    @Schema(description = "已绑定食客数")
    private long boundDiners;

    @Schema(description = "待确认预约数")
    private long pendingOrders;

    @Schema(description = "近 7 日预约趋势")
    private List<TrendPoint> reservationTrend = new ArrayList<>();

    @Schema(description = "近 7 日热门菜")
    private List<HotDishPoint> hotDishes = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "ChefStatsTrendPoint")
    public static class TrendPoint {
        private String date;
        private long count;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "ChefStatsHotDishPoint")
    public static class HotDishPoint {
        private String name;
        private long count;
    }
}
