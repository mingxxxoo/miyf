package cn.miyf.auth.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 微信登录请求。仅 code 必填；资料字段可选（兼容旧客户端补全）。
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
@Schema(name = "WxLoginDto", description = "微信登录请求")
public class WxLoginDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "微信临时登录 code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Size(max = 64)
    @Schema(description = "用户名，可选；缺省时服务端按 openid 生成")
    private String username;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号，可选")
    private String phone;

    @Size(max = 64)
    @Schema(description = "微信号，可选")
    private String wechatId;

    @Size(max = 64)
    @Schema(description = "昵称，可选；缺省时使用用户名")
    private String nickname;

    @Schema(description = "头像 URL，可选")
    private String avatarUrl;
}
