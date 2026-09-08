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
 * 权限组实体（sys_perm_group）。
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
@TableName("sys_perm_group")
@Schema(name = "SysPermGroupEntity", description = "权限组")
public class SysPermGroupEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "权限组编码")
    private String code;

    @TableField("name")
    @Schema(description = "权限组名称")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("product")
    @Schema(description = "默认产品域")
    private String product;
}
