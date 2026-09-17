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
 * 食客端自然语言预约草稿请求。
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
@Schema(name = "OrderDraftAiDto", description = "AI 预约草稿请求")
public class OrderDraftAiDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "食客自然语言", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;
}
