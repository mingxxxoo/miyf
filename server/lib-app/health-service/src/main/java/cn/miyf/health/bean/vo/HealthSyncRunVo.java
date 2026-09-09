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

import java.time.Instant;

/**
 * 健康同步运行记录对外视图。
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
@Schema(name = "HealthSyncRunVo", description = "健康同步运行记录")
public class HealthSyncRunVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", type = "string")
    private Long id;

    @Schema(description = "数据源编码")
    private String providerCode;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "拉取条数")
    private Integer fetchedCount;

    @Schema(description = "入库条数")
    private Integer ingestedCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "开始时间")
    private Instant startedTime;

    @Schema(description = "结束时间")
    private Instant finishedTime;
}
