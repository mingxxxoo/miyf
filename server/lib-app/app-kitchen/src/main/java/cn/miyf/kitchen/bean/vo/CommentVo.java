package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;

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
 * 评价展示 VO。
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
@Schema(name = "CommentVo", description = "评价")
public class CommentVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "评价 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户 ID", type = "string")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID", type = "string")
    private Long dishId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约单 ID", type = "string")
    private Long orderId;

    @Schema(description = "评分 1~5")
    private Integer rating;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "状态 NORMAL/HIDDEN")
    private String status;

    @Schema(description = "图片")
    private List<String> images;

    @Schema(description = "创建时间")
    private Instant createdAt;
}
