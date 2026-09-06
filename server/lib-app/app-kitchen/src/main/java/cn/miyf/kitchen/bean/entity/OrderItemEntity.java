package cn.miyf.kitchen.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
import java.io.Serializable;
import java.time.Instant;

/**
 * 预约明细表实体。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("kitchen_order_item")
@Schema(name = "OrderItemEntity", description = "预约明细")
public class OrderItemEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花）", type = "string")
    private Long id;

    @TableField("order_id")
    @Schema(description = "预约单 ID")
    private Long orderId;

    @TableField("dish_id")
    @Schema(description = "菜品 ID")
    private Long dishId;

    @TableField("dish_name")
    @Schema(description = "菜品名称快照")
    private String dishName;

    @TableField("quantity")
    @Schema(description = "数量")
    private Integer quantity;

    @TableField("unit")
    @Schema(description = "单位")
    private String unit;

    @TableField("remark")
    @Schema(description = "明细备注")
    private String remark;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private Instant createdAt;
}
