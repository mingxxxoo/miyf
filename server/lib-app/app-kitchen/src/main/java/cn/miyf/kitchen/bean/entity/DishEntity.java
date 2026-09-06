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
import java.math.BigDecimal;

/**
 * 菜品表实体；不含价格字段。
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
@TableName("dish")
@Schema(name = "DishEntity", description = "菜品")
public class DishEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("category_id")
    @Schema(description = "分类 ID")
    private Long categoryId;

    @TableField("name")
    @Schema(description = "菜品名称")
    private String name;

    @TableField("subtitle")
    @Schema(description = "副标题")
    private String subtitle;

    @TableField("description")
    @Schema(description = "简介")
    private String description;

    @TableField("cover_image")
    @Schema(description = "封面图 URL")
    private String coverImage;

    @TableField("status")
    @Schema(description = "状态：DRAFT/ON_SALE/OFF_SALE")
    private String status;

    @TableField("sort_order")
    @Schema(description = "排序值")
    private Integer sortOrder;

    @TableField("is_recommend")
    @Schema(description = "是否推荐")
    private Boolean recommend;

    @TableField("stock")
    @Schema(description = "可提供份数（非商业库存）")
    private Integer stock;

    @TableField("stock_type")
    @Schema(description = "份数模式：LIMITED/UNLIMITED")
    private String stockType;

    @TableField("unit")
    @Schema(description = "单位，默认份")
    private String unit;

    @TableField("rating")
    @Schema(description = "平均评分，系统维护")
    private BigDecimal rating;

    @TableField("rating_count")
    @Schema(description = "有效评价数，系统维护")
    private Integer ratingCount;

    @TableField("created_by")
    @Schema(description = "创建人")
    private Long createdBy;

    @TableField("updated_by")
    @Schema(description = "更新人")
    private Long updatedBy;
}
