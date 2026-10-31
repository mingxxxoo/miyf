package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
 * 提交做菜任务请求。
 * <p>
 * 任务创建时固化菜品快照；{@code submitToken} 用于重复点击/网络重试幂等，
 * 同一令牌只会落一条任务。
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
@Schema(name = "CookingTaskCreateDto", description = "提交做菜任务")
public class CookingTaskCreateDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "菜品模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @Min(1)
    @Max(99)
    @Schema(description = "份数，默认取菜品建议份数")
    private Integer servings;

    @Size(max = 500)
    @Schema(description = "备注，如口味偏好、忌口")
    private String note;

    @Min(0)
    @Max(9)
    @Schema(description = "优先级 0-9，越大越优先，默认 0")
    private Integer priority;

    @Schema(description = "计划开始时间，可空")
    private Instant scheduledStartTime;

    @Size(max = 64)
    @Schema(description = "提交幂等令牌，建议前端每次进入提交页生成一次")
    private String submitToken;
}
