package cn.miyf.kitchen.bean.qo;

import cn.miyf.common.query.AbstractCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 厨房分页查询。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Schema(name = "KitchenPageQo")
public class KitchenPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    /** OPEN/CLOSED/BANNED */
    private String status;
}
