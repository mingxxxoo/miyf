package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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
 * 做菜任务步骤展示 VO：快照内容 + 步骤计时状态。
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
@Schema(name = "CookingTaskStepVo", description = "做菜任务步骤")
public class CookingTaskStepVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "步骤记录 ID", type = "string")
    private Long id;

    @Schema(description = "步骤序号，从 1 开始")
    private Integer stepOrder;

    @Schema(description = "步骤状态：PENDING/RUNNING/PAUSED/DONE")
    private String status;

    @Schema(description = "计时方式：countdown/countup/none")
    private String timerType;

    @Schema(description = "预计时长（秒）")
    private Integer durationSeconds;

    @Schema(description = "倒计时剩余秒数；按服务端时间实时计算")
    private Integer remainingSeconds;

    @Schema(description = "已计时秒数；按服务端时间实时计算")
    private Integer elapsedSeconds;

    @Schema(description = "步骤开始时间")
    private Instant startedTime;

    @Schema(description = "暂停开始时间")
    private Instant pausedTime;

    @Schema(description = "累计暂停秒数")
    private Integer totalPausedSeconds;

    @Schema(description = "完成时间")
    private Instant completedTime;

    @Schema(description = "步骤标题")
    private String title;

    @Schema(description = "操作说明")
    private String description;

    @Schema(description = "步骤图片")
    private String image;

    @Schema(description = "步骤视频")
    private String video;

    @Schema(description = "并行分组")
    private String parallelGroup;

    @Schema(description = "完成提示")
    private String completionHint;

    @Schema(description = "风险提示")
    private String warning;

    @Schema(description = "是否必需步骤")
    private Boolean required;

    @Schema(description = "是否允许撤销")
    private Boolean canUndo;

    @Schema(description = "是否为当前步骤")
    private Boolean current;

    @Schema(description = "当前用户是否可开始本步骤")
    private Boolean canStart;

    @Schema(description = "当前用户是否可暂停本步骤")
    private Boolean canPause;

    @Schema(description = "当前用户是否可继续本步骤")
    private Boolean canResume;

    @Schema(description = "当前用户是否可完成本步骤")
    private Boolean canComplete;

    @Schema(description = "当前用户是否可撤销本步骤")
    private Boolean canUndoNow;
}
