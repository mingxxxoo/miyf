package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 评论图片表实体。
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
@TableName("kitchen_comment_image")
@Schema(name = "CommentImageEntity", description = "评价图片")
public class CommentImageEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("comment_id")
    @Schema(description = "评论 ID")
    private Long commentId;

    @TableField("image_url")
    @Schema(description = "图片 URL")
    private String url;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
}
