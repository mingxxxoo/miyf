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
import java.time.Instant;

/**
 * 厨师–食客绑定实体。
 * 状态：PENDING / BOUND / REJECTED / UNBOUND；一客一厨。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("kitchen_binding")
@Schema(name = "KitchenBindingEntity", description = "厨房绑定")
public class KitchenBindingEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 厨房 ID */
    @TableField("kitchen_id")
    private Long kitchenId;

    /** 食客用户 ID */
    @TableField("diner_user_id")
    private Long dinerUserId;

    /** PENDING/BOUND/REJECTED/UNBOUND */
    @TableField("status")
    private String status;

    /** 拒绝或厨师取消待确认时的原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 申请时间 */
    @TableField("applied_at")
    private Instant appliedAt;

    /** 通过/拒绝时间 */
    @TableField("decided_at")
    private Instant decidedAt;

    /** 解绑时间 */
    @TableField("unbound_at")
    private Instant unboundAt;
}
