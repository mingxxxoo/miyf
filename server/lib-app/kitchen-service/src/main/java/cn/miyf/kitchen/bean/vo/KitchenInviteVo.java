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
 * 邀请码展示。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Accessors(chain = true)
@Schema(name = "KitchenInviteVo")
public class KitchenInviteVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long kitchenId;
    /** 短码 */
    private String code;
    /** 长 token */
    private String token;
    /** 小程序加入页路径 */
    private String joinPath;
    private Instant expireAt;
    private Integer maxUses;
    private Integer usedCount;
    private String status;
}
