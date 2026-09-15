package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 菜品审核驳回请求。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Schema(name = "DishAuditRejectDto")
public class DishAuditRejectDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 驳回原因（必填） */
    @NotBlank
    private String reason;
}
