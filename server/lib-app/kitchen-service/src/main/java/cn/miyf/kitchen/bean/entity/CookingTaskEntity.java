package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
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
import java.util.Map;

/**
 * 做菜任务表实体。
 * <p>
 * {@code dish_snapshot} 在创建任务时固化菜品与步骤，菜品模板后续修改不影响任务。
 * 步骤级计时落在 {@link CookingTaskStepEntity}，本表仅保留任务级时间与汇总。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "cooking_task", autoResultMap = true)
@Schema(name = "CookingTaskEntity", description = "做菜任务")
public class CookingTaskEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("kitchen_id")
    @Schema(description = "所属厨房（组织）")
    private Long kitchenId;

    @TableField("org_unit_id")
    @Schema(description = "所属组织单元 ID（任务池按组织隔离）")
    private Long orgUnitId;

    @TableField("dish_id")
    @Schema(description = "菜品模板 ID")
    private Long dishId;

    @TableField(value = "dish_snapshot", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "菜品快照")
    private Map<String, Object> dishSnapshot;

    @TableField("submitter_id")
    @Schema(description = "提交人")
    private Long submitterId;

    @TableField("chef_id")
    @Schema(description = "领取厨师，未领取为空")
    private Long chefId;

    @TableField("servings")
    @Schema(description = "份数")
    private Integer servings;

    @TableField("note")
    @Schema(description = "备注")
    private String note;

    @TableField("priority")
    @Schema(description = "优先级，0-9，越大越优先")
    private Integer priority;

    @TableField("claim_mode")
    @Schema(description = "领取方式：PUBLIC 公开任务池 / DIRECT 指定厨师")
    private String claimMode;

    @TableField("scheduled_start_time")
    @Schema(description = "计划开始时间")
    private Instant scheduledStartTime;

    @TableField("status")
    @Schema(description = "状态：PENDING/CLAIMED/PREPARING/COOKING/PAUSED/COMPLETED/CANCELLED")
    private String status;

    @TableField("current_step")
    @Schema(description = "当前步骤序号，0 表示未开始")
    private Integer currentStep;

    @TableField("started_time")
    @Schema(description = "开始制作时间")
    private Instant startedTime;

    @TableField("paused_time")
    @Schema(description = "当前暂停开始时间")
    private Instant pausedTime;

    @TableField("completed_time")
    @Schema(description = "完成时间")
    private Instant completedTime;

    @TableField("total_paused_seconds")
    @Schema(description = "累计暂停秒数")
    private Integer totalPausedSeconds;

    @TableField("actual_duration_seconds")
    @Schema(description = "实际用时（秒）")
    private Integer actualDurationSeconds;

    @TableField("submit_token")
    @Schema(description = "提交幂等令牌")
    private String submitToken;
}
