package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 选择或切换身份请求。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Schema(name = "RoleChooseDto")
public class RoleChooseDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    /** CHEF 或 DINER */
    @NotBlank
    @Schema(description = "CHEF 或 DINER")
    private String role;
}
