package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 菜谱表实体；JSONB 仅在 Infrastructure 使用 TypeHandler。
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
@TableName(value = "dish_recipe", autoResultMap = true)
@Schema(name = "RecipeEntity", description = "菜谱")
public class RecipeEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("dish_id")
    @Schema(description = "菜品 ID")
    private Long dishId;

    @TableField("description")
    @Schema(description = "菜谱描述")
    private String description;

    @TableField("difficulty")
    @Schema(description = "难度")
    private String difficulty;

    @TableField("prepare_minutes")
    @Schema(description = "准备分钟数")
    private Integer prepareMinutes;

    @TableField("cook_minutes")
    @Schema(description = "烹饪分钟数")
    private Integer cookMinutes;

    @TableField("servings")
    @Schema(description = "份量")
    private Integer servings;

    @TableField(value = "ingredients", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "食材 JSON")
    private List<Map<String, Object>> ingredients;

    @TableField(value = "seasonings", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "调味料 JSON")
    private List<Map<String, Object>> seasonings;

    @TableField(value = "steps", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "步骤 JSON")
    private List<Map<String, Object>> steps;

    @TableField("tips")
    @Schema(description = "小贴士")
    private String tips;

    @TableField(value = "nutrition", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "营养信息 JSON")
    private Map<String, Object> nutrition;
}
