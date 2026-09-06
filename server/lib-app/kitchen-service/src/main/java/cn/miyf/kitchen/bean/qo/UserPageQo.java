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
 * 厨房用户分页查询条件。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "UserPageQo", description = "厨房用户分页查询")
public class UserPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
