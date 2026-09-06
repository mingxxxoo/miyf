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
 * 评价分页查询条件。
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
@Schema(name = "CommentPageQo", description = "评价分页查询")
public class CommentPageQo extends AbstractCondition {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "状态：NORMAL/HIDDEN")
    private String status;

    @Schema(description = "星级 1~5")
    private Integer rating;

    @Schema(description = "用户端排序：latest / rating_asc / rating_desc")
    private String sortMode;
}
