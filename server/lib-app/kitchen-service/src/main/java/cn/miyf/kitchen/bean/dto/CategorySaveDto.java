package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;

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

/**
 * 分类保存请求。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:13
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "CategorySaveDto", description = "分类保存请求")
public class CategorySaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序值，越小越靠前")
    private Integer sortOrder;

    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;
}
