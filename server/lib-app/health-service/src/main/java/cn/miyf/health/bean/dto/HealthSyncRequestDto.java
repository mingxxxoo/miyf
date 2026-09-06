package cn.miyf.health.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Set;

/**
 * 触发数据源同步。
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
@Schema(name = "HealthSyncRequestDto")
public class HealthSyncRequestDto {

    @NotBlank
    @Schema(description = "主体 ID")
    private String subjectId;

    @Schema(description = "起始时间")
    private Instant from;

    @Schema(description = "结束时间")
    private Instant to;

    @Schema(description = "指标过滤")
    private Set<String> metricCodes;
}
