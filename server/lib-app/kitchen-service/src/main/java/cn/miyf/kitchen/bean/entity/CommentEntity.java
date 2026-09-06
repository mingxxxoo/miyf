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
 * 评论表实体。
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
@TableName("kitchen_comment")
@Schema(name = "CommentEntity", description = "菜品评价")
public class CommentEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    @Schema(description = "用户 ID")
    private Long userId;

    @TableField("dish_id")
    @Schema(description = "菜品 ID")
    private Long dishId;

    @TableField("order_id")
    @Schema(description = "预约单 ID")
    private Long orderId;

    @TableField("rating")
    @Schema(description = "评分 1~5")
    private Integer rating;

    @TableField("content")
    @Schema(description = "评价内容，可空")
    private String content;

    @TableField("status")
    @Schema(description = "状态：NORMAL/HIDDEN")
    private String status;
}
