package cn.miyf.ai.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 食客端指标满足度分析请求。
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
@Schema(name = "OrderInsightAiDto", description = "AI 饮食参考分析请求")
public class OrderInsightAiDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "食客原始诉求文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dinerText;

    @NotBlank
    @Schema(description = "预约草稿 JSON（调用①输出）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String draftJson;
}
