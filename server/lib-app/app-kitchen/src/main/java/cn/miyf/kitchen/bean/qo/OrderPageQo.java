package cn.miyf.kitchen.bean.qo;

import cn.miyf.common.query.AbstractCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 预约单分页查询条件。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrderPageQo", description = "预约单分页查询")
public class OrderPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "预约单号")
    private String orderNo;

    @Schema(description = "用户 ID")
    private Long userId;
}
