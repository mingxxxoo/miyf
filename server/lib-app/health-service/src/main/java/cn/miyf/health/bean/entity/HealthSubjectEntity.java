package cn.miyf.health.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
import java.time.LocalDate;

/**
 * 健康主体（被采集人）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("health_subject")
@Schema(name = "HealthSubjectEntity", description = "健康主体")
public class HealthSubjectEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("display_name")
    @Schema(description = "显示名")
    private String displayName;

    @TableField("gender")
    @Schema(description = "性别 UNKNOWN/MALE/FEMALE")
    private String gender;

    @TableField("birth_date")
    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @TableField("height_cm")
    @Schema(description = "身高 cm")
    private BigDecimal heightCm;

    @TableField("external_user_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "外部用户 ID（如厨房用户）", type = "string")
    private Long externalUserId;

    @TableField("status")
    @Schema(description = "状态")
    private String status;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
}
