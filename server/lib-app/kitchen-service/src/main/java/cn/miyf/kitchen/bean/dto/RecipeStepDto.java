package cn.miyf.kitchen.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜谱制作步骤。
 * <p>
 * 组织协作做菜新增时长、计时方式、并行分组、完成提示、风险提示与撤销开关；
 * 老数据只带 step/title/description/image，新字段为空。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "RecipeStepDto", description = "制作步骤")
public class RecipeStepDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤序号，可空，服务端按列表顺序重排")
    private Integer step;

    @Schema(description = "步骤顺序，可空，服务端按列表顺序重排；与 step 同值")
    private Integer order;

    @Schema(description = "步骤标题")
    private String title;

    @NotBlank
    @Schema(description = "步骤说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "步骤图片 URL")
    private String image;

    @Schema(description = "步骤视频 URL")
    private String video;

    @Schema(description = "预计时长（秒），无法确认时为空，不猜测")
    private Integer durationSeconds;

    @Schema(description = "计时方式：countdown/countup/none")
    private String timerType;

    @Schema(description = "并行分组，相同分组可同时进行")
    private String parallelGroup;

    @Schema(description = "完成提示")
    private String completionHint;

    @Schema(description = "风险提示，如火候、溅油")
    private String warning;

    @Schema(description = "是否必需步骤，默认 true")
    private Boolean required;

    @Schema(description = "是否允许撤销，默认 true")
    private Boolean canUndo;

    /**
     * 兼容旧代码的四元构造。
     *
     * @param step        步骤序号
     * @param title       标题
     * @param description 说明
     * @param image       图片
     */
    public RecipeStepDto(Integer step, String title, String description, String image) {
        this.step = step;
        this.title = title;
        this.description = description;
        this.image = image;
    }
}
