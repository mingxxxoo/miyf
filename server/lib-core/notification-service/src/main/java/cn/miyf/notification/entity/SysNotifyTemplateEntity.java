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
 * 通知模板（sys_notify_template）。
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
@TableName("sys_notify_template")
@Schema(name = "SysNotifyTemplateEntity", description = "通知模板")
public class SysNotifyTemplateEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "模板编码")
    private String code;

    @TableField("name")
    @Schema(description = "模板名称")
    private String name;

    @TableField("channel")
    @Schema(description = "渠道 EMAIL/SMS/INBOX")
    private String channel;

    @TableField("title_template")
    @Schema(description = "标题模板")
    private String titleTemplate;

    @TableField("content_template")
    @Schema(description = "正文模板")
    private String contentTemplate;

    @TableField("status")
    @Schema(description = "ENABLED/DISABLED")
    private String status;
}
