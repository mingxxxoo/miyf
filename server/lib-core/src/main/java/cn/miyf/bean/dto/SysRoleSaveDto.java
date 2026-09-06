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

import java.util.List;

/**
 * 角色保存 DTO。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysRoleSaveDto", description = "角色保存")
public class SysRoleSaveDto {

    @NotBlank
    @Schema(description = "角色码")
    private String code;

    @NotBlank
    @Schema(description = "角色名")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "权限组 ID 列表")
    private List<Long> groupIds;
}
