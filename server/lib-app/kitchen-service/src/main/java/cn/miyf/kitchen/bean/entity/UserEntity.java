package cn.miyf.kitchen.bean.entity;

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
 * 用户表实体。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("kitchen_user")
@Schema(name = "UserEntity", description = "微信用户")
public class UserEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("openid")
    @Schema(description = "微信 openid")
    private String openid;

    @TableField("unionid")
    @Schema(description = "微信 unionid")
    private String unionid;

    @TableField("nickname")
    @Schema(description = "昵称")
    private String nickname;

    @TableField("avatar_url")
    @Schema(description = "头像 URL")
    private String avatarUrl;

    @TableField("status")
    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
