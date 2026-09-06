package cn.miyf.health.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 手动录入采样。
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
@Schema(name = "HealthSampleSaveDto")
public class HealthSampleSaveDto {

    @NotBlank
    @Schema(description = "主体 ID")
    private String subjectId;

    @NotBlank
    @Schema(description = "指标编码")
    private String metricCode;

    @NotNull
    @Schema(description = "数值")
    private BigDecimal valueNum;

    @Schema(description = "单位，空则用默认")
    private String unit;

    @NotNull
    @Schema(description = "测量时间")
    private Instant measuredAt;

    @Schema(description = "质量")
    private String quality;

    @Schema(description = "扩展 JSON")
    private String metaJson;
}
