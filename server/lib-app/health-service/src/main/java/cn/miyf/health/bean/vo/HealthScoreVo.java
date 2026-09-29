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

/**
 * 个人健康综合评分（生活方式参考，非诊断）。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthScoreVo", description = "健康综合评分")
public class HealthScoreVo {

    @Schema(description = "综合分 0～98；无数据为 0")
    private int score;

    @Schema(description = "状态文案")
    private String title;

    @Schema(description = "有数据的指标数")
    private int withData;

    @Schema(description = "需关注指标数")
    private int alerts;

    @Schema(description = "正常指标数")
    private int normal;

    @Schema(description = "首个需关注项，可空")
    private AlertItem firstAlert;

    @Schema(description = "免责声明")
    private String disclaimer;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Schema(name = "HealthScoreAlertItem")
    public static class AlertItem {
        private String metricCode;
        private String label;
        private String level;
        private String message;
        private BigDecimal latest;
        private String unit;
    }
}
