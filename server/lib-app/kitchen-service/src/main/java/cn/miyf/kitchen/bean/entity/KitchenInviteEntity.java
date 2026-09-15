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
 * 厨房邀请码实体。
 * ACTIVE 有效；换码后旧记录 REVOKED。
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
@TableName("kitchen_invite")
@Schema(name = "KitchenInviteEntity", description = "厨房邀请")
public class KitchenInviteEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 厨房 ID */
    @TableField("kitchen_id")
    private Long kitchenId;

    /** 8 位短码 */
    @TableField("code")
    private String code;

    /** 长 token，用于小程序 join 链接 */
    @TableField("token")
    private String token;

    /** 过期时间，空表示不过期 */
    @TableField("expire_at")
    private Instant expireAt;

    /** 最大使用次数，空表示不限 */
    @TableField("max_uses")
    private Integer maxUses;

    /** 已使用次数 */
    @TableField("used_count")
    private Integer usedCount;

    /** ACTIVE/REVOKED 等 */
    @TableField("status")
    private String status;
}
