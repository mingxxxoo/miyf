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
 * 做菜任务分页查询。
 * <p>
 * scope 取值：pool 任务池（默认）、submitted 我提交的、assigned 我领取的、all 组织全部。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "CookingTaskPageQo", description = "做菜任务分页查询")
public class CookingTaskPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "范围：pool/submitted/assigned/all，默认 pool")
    private String scope;

    @Schema(description = "状态：PENDING/CLAIMED/PREPARING/COOKING/PAUSED/COMPLETED/CANCELLED")
    private String status;

    @Schema(description = "关键字，匹配菜品名/备注（仅任务池内存筛选）")
    private String keyword;
}
