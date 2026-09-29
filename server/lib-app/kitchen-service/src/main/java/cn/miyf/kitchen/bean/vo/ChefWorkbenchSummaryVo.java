package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 厨师工作台首页汇总（角标 + 最新预约），避免前端多次列表扇出。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChefWorkbenchSummaryVo", description = "厨师工作台汇总")
public class ChefWorkbenchSummaryVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "厨房 ID", type = "string")
    private Long kitchenId;

    @Schema(description = "厨房名称")
    private String kitchenName;

    @Schema(description = "厨房状态 OPEN/CLOSED 等")
    private String kitchenStatus;

    @Schema(description = "待确认预约数")
    private long pendingOrders;

    @Schema(description = "备餐中预约数")
    private long preparingOrders;

    @Schema(description = "可取餐预约数")
    private long readyOrders;

    @Schema(description = "在售菜品数")
    private long onSaleDishes;

    @Schema(description = "已绑定食客数")
    private long boundDiners;

    @Schema(description = "待确认食客申请数")
    private long pendingBindings;

    @Schema(description = "最新预约（默认最多 5 条）")
    private List<OrderVo> recentOrders = new ArrayList<>();
}
