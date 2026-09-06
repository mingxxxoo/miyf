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
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 菜谱展示 VO。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "RecipeVo", description = "菜谱展示")
public class RecipeVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜谱 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID", type = "string")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "难度")
    private String difficulty;

    @Schema(description = "准备分钟数")
    private Integer prepareMinutes;

    @Schema(description = "烹饪分钟数")
    private Integer cookMinutes;

    @Schema(description = "建议份量")
    private Integer servings;

    @Schema(description = "食材")
    private List<Map<String, Object>> ingredients;

    @Schema(description = "调味料")
    private List<Map<String, Object>> seasonings;

    @Schema(description = "步骤")
    private List<Map<String, Object>> steps;

    @Schema(description = "小贴士")
    private String tips;

    @Schema(description = "营养信息")
    private Map<String, Object> nutrition;

    @Schema(description = "更新时间")
    private Instant updatedAt;
}
