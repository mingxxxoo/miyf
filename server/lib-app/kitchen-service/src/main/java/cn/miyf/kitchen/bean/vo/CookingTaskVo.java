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
import java.util.List;
import java.util.Map;

/**
 * 做菜任务展示 VO。
 * <p>
 * {@code dishSnapshot} 是任务创建时固化的菜品内容，任务详情不再依赖菜品模板当前状态；
 * 操作按钮是否可执行由服务端按当前用户与任务状态计算，前端不自行推断。
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
@Schema(name = "CookingTaskVo", description = "做菜任务")
public class CookingTaskVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "任务 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "厨房 ID", type = "string")
    private Long kitchenId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "组织单元 ID", type = "string")
    private Long orgUnitId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品模板 ID", type = "string")
    private Long dishId;

    @Schema(description = "菜品快照（含原料与步骤）")
    private Map<String, Object> dishSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "提交人 ID", type = "string")
    private Long submitterId;

    @Schema(description = "提交人昵称")
    private String submitterName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "领取厨师 ID", type = "string")
    private Long chefId;

    @Schema(description = "领取厨师昵称")
    private String chefName;

    @Schema(description = "份数")
    private Integer servings;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "优先级 0-9")
    private Integer priority;

    @Schema(description = "领取方式：PUBLIC/DIRECT")
    private String claimMode;

    @Schema(description = "计划开始时间")
    private Instant scheduledStartTime;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "当前步骤序号，0 表示未开始")
    private Integer currentStep;

    @Schema(description = "开始制作时间")
    private Instant startedTime;

    @Schema(description = "暂停开始时间")
    private Instant pausedTime;

    @Schema(description = "完成时间")
    private Instant completedTime;

    @Schema(description = "累计暂停秒数")
    private Integer totalPausedSeconds;

    @Schema(description = "实际用时（秒）")
    private Integer actualDurationSeconds;

    @Schema(description = "提交幂等令牌")
    private String submitToken;

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "更新时间")
    private Instant lastModifyTime;

    @Schema(description = "步骤计时状态；列表不返回时为空")
    private List<CookingTaskStepVo> steps;

    @Schema(description = "服务端当前时间，用于前端校准倒计时")
    private Instant serverTime;

    @Schema(description = "当前用户是否可领取")
    private Boolean canClaim;

    @Schema(description = "当前用户是否可开始制作")
    private Boolean canStart;

    @Schema(description = "当前用户是否可暂停")
    private Boolean canPause;

    @Schema(description = "当前用户是否可恢复")
    private Boolean canResume;

    @Schema(description = "当前用户是否可完成")
    private Boolean canComplete;

    @Schema(description = "当前用户是否可释放回任务池")
    private Boolean canRelease;

    @Schema(description = "当前用户是否可取消")
    private Boolean canCancel;

    @Schema(description = "是否由当前用户领取")
    private Boolean claimedByMe;

    @Schema(description = "当前用户是否可进入做菜流程")
    private Boolean canOpenFlow;
}
