package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import cn.miyf.oss.security.FileAccess;
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

/**
 * 菜品模板（候选菜）展示 VO。
 * <p>
 * 字段对齐设计文档：模板状态 draft/pre_created/published/archived，
 * 预计总时长以秒返回，原材料与步骤带处理方式、时长与计时方式。
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
@Schema(name = "DishTemplateVo", description = "菜品模板")
public class DishTemplateVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品模板 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属厨房 ID（创建人厨房）", type = "string")
    private Long kitchenId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属组织单位 ID", type = "string")
    private Long orgUnitId;

    @Schema(description = "菜品名称")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @FileAccess
    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "菜品介绍")
    private String description;

    @Schema(description = "建议份数")
    private Integer servings;

    @Schema(description = "难度：EASY/MEDIUM/HARD")
    private String difficulty;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "模板状态：draft/pre_created/published/archived")
    private String status;

    @Schema(description = "来源：MANUAL/AI_TEXT/AI_IMAGE/AI_URL")
    private String sourceType;

    @Schema(description = "来源网页地址")
    private String sourceUrl;

    @Schema(description = "预计总时长（秒）")
    private Integer estimatedTotalSeconds;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "创建人 ID", type = "string")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorName;

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "更新时间")
    private Instant lastModifyTime;

    @Schema(description = "当前用户是否可编辑")
    private Boolean editable;

    @Schema(description = "当前用户是否可发布")
    private Boolean publishable;

    @Schema(description = "当前用户是否可归档")
    private Boolean archivable;

    @Schema(description = "历史做菜次数")
    private Integer usageCount;

    @Schema(description = "是否做过（历史完成过做菜任务）")
    private Boolean made;

    @Schema(description = "原材料")
    private List<RecipeMaterialDto> ingredients;

    @Schema(description = "做菜步骤")
    private List<RecipeStepDto> steps;
}
