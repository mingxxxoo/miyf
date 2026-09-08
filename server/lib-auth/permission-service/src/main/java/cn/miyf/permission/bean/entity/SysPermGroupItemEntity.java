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
 * 权限组-权限关联（sys_perm_group_item），无软删。
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
@TableName("sys_perm_group_item")
@Schema(name = "SysPermGroupItemEntity", description = "权限组条目")
public class SysPermGroupItemEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("group_id")
    @Schema(description = "权限组 ID")
    private Long groupId;

    @TableField("permission_id")
    @Schema(description = "权限 ID")
    private Long permissionId;
}
