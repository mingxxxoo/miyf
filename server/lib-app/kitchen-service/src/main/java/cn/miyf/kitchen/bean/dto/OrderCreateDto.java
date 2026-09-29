package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户创建预约请求。
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
@Schema(name = "OrderCreateDto", description = "创建预约")
public class OrderCreateDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "整单备注（口味偏好等，勿再塞餐次信息）")
    private String remark;

    @Schema(description = "期望用餐日期 yyyy-MM-dd")
    private LocalDate mealDate;

    @Schema(description = "餐次 BREAKFAST/LUNCH/DINNER/SNACK/OTHER")
    private String mealType;

    @Schema(description = "期望用餐时间 HH:mm")
    private String mealTime;

    @Schema(description = "用餐人数")
    private Integer guestCount;

    @NotEmpty
    @Valid
    @Schema(description = "预约明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemCreateDto> items;
}
