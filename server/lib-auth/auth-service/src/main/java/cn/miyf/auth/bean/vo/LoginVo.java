package cn.miyf.auth.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 登录成功响应。
 * 含 token、展示信息、头像 URL、权限与角色等。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 * @history 1.00 2026-09-04 17:06 XieMingJie Created.
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "LoginVo", description = "登录结果")
public class LoginVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "过期秒数")
    private Long expireSeconds;

    @Schema(description = "主体 ID")
    private Long userId;

    @Schema(description = "展示名")
    private String displayName;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "主体类型：USER/ADMIN")
    private String principalType;

    @Schema(description = "权限码列表（管理员有值）")
    private List<String> permissions;

    @Schema(description = "角色码列表（管理员有值）")
    private List<String> roles;

    @Schema(description = "登录风控状态是否已清理；false 表示 Redis 清理失败，可能短暂仍需验证码")
    private Boolean protectStateCleared;
}
