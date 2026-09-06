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
 * 预约单表实体。
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
@TableName("kitchen_order")
@Schema(name = "OrderEntity", description = "菜品预约单")
public class OrderEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("order_no")
    @Schema(description = "预约单号")
    private String orderNo;

    @TableField("user_id")
    @Schema(description = "用户 ID")
    private Long userId;

    @TableField("status")
    @Schema(description = "状态")
    private String status;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
}
