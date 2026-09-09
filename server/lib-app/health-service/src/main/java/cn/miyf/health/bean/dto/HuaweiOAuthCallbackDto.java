package cn.miyf.health.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 华为 OAuth 回调请求。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HuaweiOAuthCallbackDto", description = "华为 OAuth 回调")
public class HuaweiOAuthCallbackDto {

    @Schema(description = "健康主体 ID（可选，缺省由 state 解析）")
    private String subjectId;

    @NotBlank
    @Schema(description = "授权码")
    private String code;

    @NotBlank
    @Schema(description = "OAuth state（必填，防 CSRF）")
    private String state;
}
