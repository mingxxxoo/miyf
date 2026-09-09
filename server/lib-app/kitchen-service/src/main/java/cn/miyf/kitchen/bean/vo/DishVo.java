package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品展示 VO。
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
@Schema(name = "DishVo", description = "菜品展示")
public class DishVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "分类 ID", type = "string")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "简介")
    private String description;

    @FileAccess
    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否推荐")
    private Boolean recommend;

    @Schema(description = "可提供份数")
    private Integer stock;

    @Schema(description = "份数模式")
    private String stockType;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "平均评分，无评价时前端显示「暂无评分」")
    private BigDecimal rating;

    @Schema(description = "评价人数")
    private Integer ratingCount;

    @FileAccess
    @Schema(description = "图片列表")
    private List<String> images;
}
