package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 菜品评分聚合结果。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "DishRatingVo", description = "菜品评分")
public class DishRatingVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "平均分，ratingCount=0 时前端显示「暂无评分」")
    private BigDecimal rating;

    @Schema(description = "有效评价数")
    private Integer ratingCount;
}
