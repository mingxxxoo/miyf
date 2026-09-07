package cn.miyf.kitchen.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
import java.io.Serializable;
import java.time.Instant;

/**
 * 评论图片表实体。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("kitchen_comment_image")
@Schema(name = "CommentImageEntity", description = "评价图片")
public class CommentImageEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花）", type = "string")
    private Long id;

    @TableField("comment_id")
    @Schema(description = "评论 ID")
    private Long commentId;

    @TableField("image_url")
    @Schema(description = "图片 URL")
    private String url;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private Instant createTime;
}
