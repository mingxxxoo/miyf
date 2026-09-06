package cn.miyf.bean.vo;

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
 * 登录风控状态（失败次数 / 验证码 / 锁定）。
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
@Schema(name = "LoginRiskVo", description = "登录风控状态")
public class LoginRiskVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "累计失败次数")
    private int failCount;

    @Schema(description = "是否需要图形验证码（失败≥3）")
    private boolean captchaRequired;

    @Schema(description = "是否临时锁定（失败≥6）")
    private boolean locked;

    @Schema(description = "锁定截止时间（epoch 毫秒），未锁定为 null")
    private Long lockedUntil;

    @Schema(description = "剩余锁定秒数")
    private long lockRemainSeconds;
}
