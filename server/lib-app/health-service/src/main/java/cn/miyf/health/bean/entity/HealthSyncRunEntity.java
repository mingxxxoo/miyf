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
import java.time.Instant;

/**
 * 健康数据同步运行记录。
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
@TableName("health_sync_run")
@Schema(name = "HealthSyncRunEntity", description = "健康同步任务")
public class HealthSyncRunEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("provider_code")
    @Schema(description = "数据源编码")
    private String providerCode;

    @TableField("subject_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @TableField("status")
    @Schema(description = "RUNNING/SUCCESS/FAILED/SKIPPED")
    private String status;

    @TableField("fetched_count")
    @Schema(description = "拉取条数")
    private Integer fetchedCount;

    @TableField("ingested_count")
    @Schema(description = "入库条数")
    private Integer ingestedCount;

    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    @TableField("started_time")
    @Schema(description = "开始时间")
    private Instant startedTime;

    @TableField("finished_time")
    @Schema(description = "结束时间")
    private Instant finishedTime;
}
