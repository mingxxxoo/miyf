package cn.miyf.notification.dto;

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
 * 通知模板保存。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "NotifyTemplateSaveDto", description = "通知模板保存")
public class NotifyTemplateSaveDto {

    @NotBlank
    @Schema(description = "编码")
    private String code;

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @NotBlank
    @Schema(description = "渠道 EMAIL/SMS/INBOX")
    private String channel;

    @NotBlank
    @Schema(description = "标题模板")
    private String titleTemplate;

    @NotBlank
    @Schema(description = "正文模板")
    private String contentTemplate;

    @Schema(description = "ENABLED/DISABLED")
    private String status;
}
