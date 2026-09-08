package cn.miyf.auth.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 管理员登录请求。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "AdminLoginDto", description = "管理员登录请求")
public class AdminLoginDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码 ID（失败≥3 次时必填）")
    private String captchaId;

    @Schema(description = "验证码内容（失败≥3 次时必填）")
    private String captchaCode;
}
