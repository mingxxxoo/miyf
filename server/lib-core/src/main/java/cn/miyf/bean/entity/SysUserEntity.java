package cn.miyf.bean.entity;

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
 * 系统用户实体（sys_user）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_user")
@Schema(name = "SysUserEntity", description = "系统用户")
public class SysUserEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("org_unit_id")
    @Schema(description = "所属单位 ID")
    private Long orgUnitId;

    @TableField("username")
    @Schema(description = "登录名")
    private String username;

    @TableField("password_hash")
    @Schema(description = "BCrypt 密码哈希")
    private String passwordHash;

    @TableField("nickname")
    @Schema(description = "昵称")
    private String nickname;

    @TableField("status")
    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
