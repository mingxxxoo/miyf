package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 用户发表评价请求。
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
@Schema(name = "CommentCreateDto", description = "发表评价")
public class CommentCreateDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "预约单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotNull
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "评分 1~5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Schema(description = "评价内容，可空")
    private String content;

    @Schema(description = "评价图片 URL 列表")
    private List<String> images;
}
