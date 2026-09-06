package cn.miyf.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 系统配置保存 DTO。
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
@Schema(name = "SysConfigSaveDto", description = "系统配置保存")
public class SysConfigSaveDto {

    @NotBlank
    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型 STRING/NUMBER/BOOLEAN/JSON")
    private String valueType;

    @Schema(description = "分组编码")
    private String groupCode;

    @NotBlank
    @Schema(description = "显示名")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @Schema(description = "排序")
    private Integer sortOrder;
}
