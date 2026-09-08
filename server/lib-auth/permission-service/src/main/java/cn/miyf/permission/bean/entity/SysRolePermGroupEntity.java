package cn.miyf.permission.bean.entity;

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
 * 角色-权限组关联（sys_role_perm_group），无软删。
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
@TableName("sys_role_perm_group")
@Schema(name = "SysRolePermGroupEntity", description = "角色权限组绑定")
public class SysRolePermGroupEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("role_id")
    @Schema(description = "角色 ID")
    private Long roleId;

    @TableField("group_id")
    @Schema(description = "权限组 ID")
    private Long groupId;
}
