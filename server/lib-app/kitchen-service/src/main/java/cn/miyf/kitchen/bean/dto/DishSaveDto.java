package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 创建/更新菜品请求。
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
@Schema(name = "DishSaveDto", description = "菜品保存请求")
public class DishSaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @NotBlank
    @Schema(description = "菜品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否推荐")
    private Boolean recommend;

    @NotNull
    @Schema(description = "份数模式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stockType;

    @Schema(description = "可提供份数，LIMITED 时有效")
    private Integer stock;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "图片列表")
    private List<String> images;
}
