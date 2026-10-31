package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 做菜任务步骤实体：保存步骤级计时状态。
 * <p>
 * 步骤内容来自任务快照的 {@code dish_snapshot.steps}，本表只存状态与计时，
 * 便于暂停/恢复/刷新后恢复倒计时。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("cooking_task_step")
@Schema(name = "CookingTaskStepEntity", description = "做菜任务步骤")
public class CookingTaskStepEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("task_id")
    @Schema(description = "任务 ID")
    private Long taskId;

    @TableField("step_order")
    @Schema(description = "步骤序号，从 1 开始")
    private Integer stepOrder;

    @TableField("status")
    @Schema(description = "步骤状态：PENDING/RUNNING/PAUSED/DONE")
    private String status;

    @TableField("timer_type")
    @Schema(description = "计时方式：countdown/countup/none")
    private String timerType;

    @TableField("duration_seconds")
    @Schema(description = "预计时长（秒）")
    private Integer durationSeconds;

    @TableField("started_time")
    @Schema(description = "步骤开始时间")
    private Instant startedTime;

    @TableField("paused_time")
    @Schema(description = "步骤暂停开始时间")
    private Instant pausedTime;

    @TableField("total_paused_seconds")
    @Schema(description = "步骤累计暂停秒数")
    private Integer totalPausedSeconds;

    @TableField("remaining_seconds")
    @Schema(description = "倒计时剩余秒数")
    private Integer remainingSeconds;

    @TableField("completed_time")
    @Schema(description = "步骤完成时间")
    private Instant completedTime;
}
