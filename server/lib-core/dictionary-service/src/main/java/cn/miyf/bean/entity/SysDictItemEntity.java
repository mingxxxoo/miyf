package cn.miyf.bean.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
 * 数据字典项实体（sys_dict_item）。
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
@TableName("sys_dict_item")
@Schema(name = "SysDictItemEntity", description = "数据字典项")
public class SysDictItemEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("type_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "字典类型 ID", type = "string")
    private Long typeId;

    @TableField("item_value")
    @Schema(description = "字典值")
    private String itemValue;

    @TableField("item_label")
    @Schema(description = "显示标签")
    private String itemLabel;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("status")
    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
}
