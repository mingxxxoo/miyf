package cn.miyf.notification.entity;

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
 * 通知发送流水（sys_notify_send_log）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_notify_send_log")
@Schema(name = "SysNotifySendLogEntity", description = "通知发送历史")
public class SysNotifySendLogEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("template_id")
    @Schema(description = "模板 ID，可空")
    private Long templateId;

    @TableField("channel")
    @Schema(description = "渠道")
    private String channel;

    @TableField("to_key")
    @Schema(description = "接收人")
    private String toKey;

    @TableField("title")
    @Schema(description = "实际标题")
    private String title;

    @TableField("content")
    @Schema(description = "实际正文")
    private String content;

    @TableField("status")
    @Schema(description = "SUCCESS/FAILED")
    private String status;

    @TableField("error")
    @Schema(description = "失败原因")
    private String error;
}
