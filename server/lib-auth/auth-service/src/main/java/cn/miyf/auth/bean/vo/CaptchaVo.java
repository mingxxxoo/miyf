package cn.miyf.auth.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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
 * 图形验证码下发。
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
@Schema(name = "CaptchaVo", description = "图形验证码")
public class CaptchaVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码 ID")
    private String captchaId;

    @Schema(description = "PNG Base64（不含 data: 前缀）")
    private String imageBase64;

    @Schema(description = "过期秒数")
    private int expireSeconds;
}
