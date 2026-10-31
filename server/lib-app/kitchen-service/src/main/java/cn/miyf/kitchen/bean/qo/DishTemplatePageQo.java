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
 * 菜品模板分页查询条件。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DishTemplatePageQo", description = "菜品模板分页查询")
public class DishTemplatePageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板状态：draft/pre_created/published/archived；为空时默认不含已归档")
    private String status;

    @Schema(description = "仅看我创建的")
    private Boolean mine;

    @Schema(description = "仅看最近使用（按做菜任务最近提交时间）")
    private Boolean recent;

    @Schema(description = "是否做过：true 已做过 / false 未做过；为空不筛选")
    private Boolean made;
}
