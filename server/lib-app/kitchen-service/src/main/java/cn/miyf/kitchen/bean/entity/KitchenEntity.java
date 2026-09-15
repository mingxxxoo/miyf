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
 * 个人厨房实体。
 * 一用户一厨；仅 OPEN 可被绑定与下单。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("kitchen")
@Schema(name = "KitchenEntity", description = "个人厨房")
public class KitchenEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    @Schema(description = "厨师用户 ID")
    private Long ownerUserId;

    @TableField("name")
    @Schema(description = "厨房名称")
    private String name;

    @TableField("intro")
    @Schema(description = "简介")
    private String intro;

    @TableField("cover_image")
    @Schema(description = "封面")
    private String coverImage;

    @TableField("status")
    @Schema(description = "OPEN/CLOSED/BANNED")
    private String status;
}
