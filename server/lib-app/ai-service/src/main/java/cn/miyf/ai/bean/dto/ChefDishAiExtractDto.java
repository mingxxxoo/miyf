package cn.miyf.ai.bean.dto;

import cn.miyf.bean.dto.BaseDto;
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
 * 厨师端 AI 建菜请求：文本与链接二选一。
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
@Schema(name = "ChefDishAiExtractDto", description = "AI 建菜抽取请求")
public class ChefDishAiExtractDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "口述/摘抄文本")
    private String text;

    @Schema(description = "网页链接（与 text 二选一，优先 text）")
    private String url;
}
