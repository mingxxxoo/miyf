package cn.miyf.auth.bean.entity;

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
 * 用户-角色关联（sys_user_role），无软删。
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
@TableName("sys_user_role")
@Schema(name = "SysUserRoleEntity", description = "用户角色绑定")
public class SysUserRoleEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    @Schema(description = "用户 ID")
    private Long userId;

    @TableField("role_id")
    @Schema(description = "角色 ID")
    private Long roleId;
}
