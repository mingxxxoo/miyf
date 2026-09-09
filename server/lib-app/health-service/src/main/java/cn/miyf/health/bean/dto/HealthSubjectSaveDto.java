package cn.miyf.health.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 健康主体保存。
 * orgUnitId 可选；非 ALL 数据范围时由服务端按当前管理员归属组织约束。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthSubjectSaveDto")
public class HealthSubjectSaveDto {

    @NotBlank
    @Schema(description = "显示名")
    private String displayName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "身高 cm")
    private BigDecimal heightCm;

    @Schema(description = "外部用户 ID")
    private String externalUserId;

    @Schema(description = "所属组织 ID（ALL 范围可指定；其他范围按当前用户约束）")
    private String orgUnitId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
