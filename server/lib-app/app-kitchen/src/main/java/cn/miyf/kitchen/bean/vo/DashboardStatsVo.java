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
 * 管理端仪表盘统计 VO。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DashboardStatsVo", description = "仪表盘统计")
public class DashboardStatsVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "启用用户数")
    private long userCount;

    @Schema(description = "今日预约数")
    private long todayOrders;

    @Schema(description = "在售菜品数")
    private long dishCount;

    @Schema(description = "隐藏评价数")
    private long pendingComments;

    @Schema(description = "近 7 日预约趋势")
    private List<TrendPoint> reservationTrend = new ArrayList<>();

    @Schema(description = "热门菜品")
    private List<HotDishPoint> hotDishes = new ArrayList<>();

    @Schema(description = "评分分布")
    private List<RatingPoint> ratingDistribution = new ArrayList<>();

    @Schema(description = "近 7 日活跃用户")
    private List<ActivityPoint> userActivity = new ArrayList<>();

    /**
     * 趋势点。
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DashboardTrendPoint", description = "预约趋势点")
    public static class TrendPoint {
        @Schema(description = "日期 MM-DD")
        private String date;
        @Schema(description = "预约数")
        private long count;
    }

    /**
     * 热门菜。
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DashboardHotDishPoint", description = "热门菜品点")
    public static class HotDishPoint {
        @Schema(description = "菜品名")
        private String name;
        @Schema(description = "份数")
        private long count;
    }

    /**
     * 评分点。
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DashboardRatingPoint", description = "评分分布点")
    public static class RatingPoint {
        @Schema(description = "星级")
        private int rating;
        @Schema(description = "数量")
        private long count;
    }

    /**
     * 活跃度点。
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "DashboardActivityPoint", description = "活跃用户点")
    public static class ActivityPoint {
        @Schema(description = "日期 MM-DD")
        private String date;
        @Schema(description = "活跃用户数")
        private long activeUsers;
    }
}
