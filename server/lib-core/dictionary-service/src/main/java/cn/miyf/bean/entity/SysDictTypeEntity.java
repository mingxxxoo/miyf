package cn.miyf.bean.entity;

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
 * 数据字典类型实体（sys_dict_type）。
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
@TableName("sys_dict_type")
@Schema(name = "SysDictTypeEntity", description = "数据字典类型")
public class SysDictTypeEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("code")
    @Schema(description = "字典编码")
    private String code;

    @TableField("name")
    @Schema(description = "字典名称")
    private String name;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("status")
    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
}
