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

    @Schema(description = "步骤标题")
    private String title;

    @NotBlank
    @Schema(description = "步骤说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "步骤图片 URL")
    private String image;
}
