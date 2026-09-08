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
 * 角色实体（sys_role）。
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
@TableName("sys_role")
@Schema(name = "SysRoleEntity", description = "角色")
public class SysRoleEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "角色码")
    private String code;

    @TableField("name")
    @Schema(description = "角色名称")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("is_default")
    @Schema(description = "是否默认角色")
    private Boolean isDefault;

    @TableField("product")
    @Schema(description = "产品域：kitchen / health / system")
    private String product;

    @TableField("data_scope")
    @Schema(description = "数据范围：ALL / ORG / ORG_CHILD / SELF")
    private String dataScope;
}
