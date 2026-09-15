package cn.miyf.kitchen.bean.qo;

import cn.miyf.common.query.AbstractCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 绑定分页查询。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Getter
@Setter
@Schema(name = "BindingPageQo")
public class BindingPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 厨房 ID（管理端过滤） */
    private Long kitchenId;
    /** PENDING/BOUND/REJECTED/UNBOUND */
    private String status;
}
