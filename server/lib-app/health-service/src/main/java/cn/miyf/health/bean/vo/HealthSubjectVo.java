package cn.miyf.health.bean.vo;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 健康主体对外视图。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthSubjectVo", description = "健康主体")
public class HealthSubjectVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", type = "string")
    private Long id;

    @Schema(description = "显示名")
    private String displayName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "身高 cm")
    private BigDecimal heightCm;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "外部用户 ID", type = "string")
    private Long externalUserId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属组织 ID", type = "string")
    private Long orgUnitId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "创建人用户 ID", type = "string")
    private Long createdBy;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "最后修改时间")
    private Instant lastModifyTime;
}
