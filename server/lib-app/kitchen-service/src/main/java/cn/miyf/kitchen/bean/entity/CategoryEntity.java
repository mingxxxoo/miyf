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
 * 菜品分类表实体。
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
@TableName("dish_category")
@Schema(name = "CategoryEntity", description = "菜品分类")
public class CategoryEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("name")
    @Schema(description = "分类名称")
    private String name;

    @TableField("icon")
    @Schema(description = "图标")
    private String icon;

    @TableField("sort_order")
    @Schema(description = "排序值，越小越靠前")
    private Integer sortOrder;

    @TableField("status")
    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
