package cn.miyf.auth.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
 * 鸿蒙华为账号登录请求。只收 authorization code，密钥留在服务端。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HuaweiLoginDto", description = "华为账号登录请求")
public class HuaweiLoginDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 512)
    @Schema(description = "Account Kit 授权码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authorizationCode;
}
