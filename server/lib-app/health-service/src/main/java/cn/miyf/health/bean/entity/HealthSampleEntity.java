package cn.miyf.health.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 健康采样点（时间序列）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("health_sample")
@Schema(name = "HealthSampleEntity", description = "健康采样")
public class HealthSampleEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("subject_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @TableField("metric_code")
    @Schema(description = "规范指标编码")
    private String metricCode;

    @TableField("value_num")
    @Schema(description = "数值")
    private BigDecimal valueNum;

    @TableField("unit")
    @Schema(description = "单位")
    private String unit;

    @TableField("measured_at")
    @Schema(description = "测量时间")
    private Instant measuredAt;

    @TableField("provider_code")
    @Schema(description = "数据源编码")
    private String providerCode;

    @TableField("source_sample_id")
    @Schema(description = "源侧采样 ID（幂等）")
    private String sourceSampleId;

    @TableField("quality")
    @Schema(description = "质量 NORMAL/ESTIMATED/SUSPECT")
    private String quality;

    @TableField("meta_json")
    @Schema(description = "扩展 JSON")
    private String metaJson;
}
