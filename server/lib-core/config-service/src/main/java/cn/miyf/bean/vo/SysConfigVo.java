package cn.miyf.bean.vo;

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

import java.time.Instant;

/**
 * 系统配置对外视图；敏感项不回传明文。
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
@Schema(name = "SysConfigVo", description = "系统配置（脱敏）")
public class SysConfigVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", type = "string")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值；敏感项为 null")
    private String configValue;

    @Schema(description = "是否敏感")
    private boolean sensitive;

    @Schema(description = "敏感项是否已配置值")
    private boolean configured;

    @Schema(description = "值类型")
    private String valueType;

    @Schema(description = "分组")
    private String groupCode;

    @Schema(description = "显示名")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "最后修改时间")
    private Instant lastModifyTime;
}
