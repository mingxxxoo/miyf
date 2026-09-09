package cn.miyf.health.bean.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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

/**
 * 健康采样对外视图（不含内部扩展字段扩散）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthSampleVo", description = "健康采样")
public class HealthSampleVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @Schema(description = "指标编码")
    private String metricCode;

    @Schema(description = "数值")
    private BigDecimal valueNum;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "测量时间")
    private Instant measuredTime;

    @Schema(description = "数据源编码")
    private String providerCode;

    @Schema(description = "质量")
    private String quality;

    @Schema(description = "创建时间")
    private Instant createTime;
}
