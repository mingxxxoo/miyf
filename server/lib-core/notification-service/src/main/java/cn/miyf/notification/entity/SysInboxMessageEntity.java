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
 * 站内信（sys_inbox_message）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_inbox_message")
@Schema(name = "SysInboxMessageEntity", description = "站内信")
public class SysInboxMessageEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_key")
    @Schema(description = "接收人标识（用户名 / 用户ID）")
    private String userKey;

    @TableField("title")
    @Schema(description = "标题")
    private String title;

    @TableField("content")
    @Schema(description = "正文")
    private String content;

    @TableField("read_flag")
    @Schema(description = "是否已读")
    private Boolean readFlag;
}
