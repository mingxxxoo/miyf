package cn.miyf.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 发送通知请求（兼容直发与模板）。
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
@Schema(name = "NotifySendDto", description = "发送通知")
public class NotifySendDto {

    @Schema(description = "渠道，模板发送时可省略")
    private String channel;

    @Schema(description = "接收人")
    private String to;

    @Schema(description = "标题（直发）")
    private String title;

    @Schema(description = "正文（直发）")
    private String content;

    @Schema(description = "模板编码，优先于 title/content")
    private String templateCode;

    @Schema(description = "模板变量")
    private Map<String, String> vars;
}
