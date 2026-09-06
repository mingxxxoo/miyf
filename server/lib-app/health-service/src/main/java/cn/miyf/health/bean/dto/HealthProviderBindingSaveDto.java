package cn.miyf.health.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 主体绑定外部数据源账号。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthProviderBindingSaveDto")
public class HealthProviderBindingSaveDto {

    @NotBlank
    @Schema(description = "数据源编码")
    private String providerCode;

    @Schema(description = "外部账号 ID")
    private String externalAccountId;

    @Schema(description = "凭证引用（不落明文）")
    private String credentialRef;

    @Schema(description = "ACTIVE/INACTIVE/REVOKED")
    private String status;
}
