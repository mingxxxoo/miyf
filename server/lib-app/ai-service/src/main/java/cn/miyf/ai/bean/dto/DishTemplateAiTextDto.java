package cn.miyf.ai.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 菜品库 AI 建菜请求（文本 / 网页链接二选一，优先文本）。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DishTemplateAiTextDto", description = "菜品库 AI 建菜请求")
public class DishTemplateAiTextDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 4000)
    @Schema(description = "菜品文字素材，最多 4000 字")
    private String text;

    @Size(max = 1024)
    @Schema(description = "网页链接（与 text 二选一，优先 text）")
    private String url;
}
