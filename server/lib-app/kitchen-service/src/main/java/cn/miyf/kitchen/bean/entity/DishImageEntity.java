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
 * 菜品图片表实体。
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
@TableName("dish_image")
@Schema(name = "DishImageEntity", description = "菜品图片")
public class DishImageEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("dish_id")
    @Schema(description = "菜品 ID")
    private Long dishId;

    @TableField("image_url")
    @Schema(description = "图片 URL")
    private String url;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
}
