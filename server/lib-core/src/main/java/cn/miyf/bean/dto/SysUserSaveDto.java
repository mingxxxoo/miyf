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
 * 系统用户保存 DTO。
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
@Schema(name = "SysUserSaveDto", description = "系统用户保存")
public class SysUserSaveDto {

    @Schema(description = "所属单位 ID")
    private Long orgUnitId;

    @NotBlank
    @Schema(description = "登录名")
    private String username;

    @Schema(description = "明文密码（创建必填，更新可空）")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;
}
