package cn.miyf.user.bean.dto;

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
 * IAM 用户启停请求。
 *
 * @author XieMingJie
 * @since 2026-09-11
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysUserStatusUpdateDto", description = "系统用户启停")
public class SysUserStatusUpdateDto {

    @NotBlank
    @Schema(description = "ENABLED / DISABLED")
    private String status;
}
