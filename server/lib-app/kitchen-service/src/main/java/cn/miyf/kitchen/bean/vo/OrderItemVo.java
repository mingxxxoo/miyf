package cn.miyf.kitchen.bean.vo;

import cn.miyf.oss.security.FileAccess;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约明细展示。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrderItemVo", description = "预约明细")
public class OrderItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID", type = "string")
    private Long dishId;

    @Schema(description = "菜品名称快照")
    private String dishName;

    @FileAccess
    @Schema(description = "菜品封面（当前菜品封面，非下单快照）")
    private String coverImage;

    @Schema(description = "份数")
    private Integer quantity;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "明细备注")
    private String remark;

    @Schema(description = "当前用户是否已评价该明细")
    private Boolean commented;
}
