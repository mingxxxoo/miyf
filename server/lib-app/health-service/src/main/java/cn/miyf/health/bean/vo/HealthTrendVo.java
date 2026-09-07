package cn.miyf.health.bean.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康指标趋势。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthTrendVo")
public class HealthTrendVo {

    private String subjectId;
    private String metricCode;
    private String unit;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal avg;
    private BigDecimal latest;
    private Instant latestTime;
    private int pointCount;
    private List<Point> points = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Point {
        private Instant measuredTime;
        private BigDecimal value;
        private String providerCode;
        private String quality;
    }
}
