package cn.miyf.kitchen.bean.dto;

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
 * 管理端更新预约状态请求。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrderStatusUpdateDto", description = "预约状态更新")
public class OrderStatusUpdateDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
