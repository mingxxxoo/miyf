package cn.miyf.ai.bean.entity;

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

/**
 * AI 调用审计日志。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("ai_call_log")
@Schema(name = "AiCallLogEntity", description = "AI 调用日志")
public class AiCallLogEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("scene")
    @Schema(description = "场景：CHEF_DISH / ORDER_DRAFT / ORDER_INSIGHT")
    private String scene;

    @TableField("user_id")
    @Schema(description = "调用用户")
    private Long userId;

    @TableField("kitchen_id")
    @Schema(description = "厨房 ID")
    private Long kitchenId;

    @TableField("model_name")
    @Schema(description = "模型名")
    private String modelName;

    @TableField("input_summary")
    @Schema(description = "输入摘要（脱敏）")
    private String inputSummary;

    @TableField("output_json")
    @Schema(description = "结构化输出 JSON")
    private String outputJson;

    @TableField("prompt_tokens")
    @Schema(description = "prompt tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    @Schema(description = "completion tokens")
    private Integer completionTokens;

    @TableField("duration_ms")
    @Schema(description = "耗时毫秒")
    private Long durationMs;

    @TableField("success")
    @Schema(description = "是否成功")
    private Boolean success;

    @TableField("error_message")
    @Schema(description = "失败原因")
    private String errorMessage;

    @TableField("adopted")
    @Schema(description = "用户是否采纳（后续回写）")
    private Boolean adopted;
}
