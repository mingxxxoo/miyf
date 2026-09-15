package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.Instant;

/**
 * 绑定关系展示。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Accessors(chain = true)
@Schema(name = "KitchenBindingVo")
public class KitchenBindingVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long kitchenId;
    private String kitchenName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dinerUserId;
    private String dinerNickname;
    /** PENDING/BOUND/REJECTED/UNBOUND */
    private String status;
    private String rejectReason;
    private Instant appliedAt;
    private Instant decidedAt;
    private Instant unboundAt;
}
