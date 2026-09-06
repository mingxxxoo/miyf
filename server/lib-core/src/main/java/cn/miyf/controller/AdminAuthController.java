package cn.miyf.controller;

import cn.miyf.service.AuthApplicationService;
import cn.miyf.common.ApiResult;
import cn.miyf.bean.dto.AdminLoginDto;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.SecurityUtils;
import cn.miyf.bean.vo.LoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Tag(name = "管理端认证")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 构造控制器。
     *
     * @param authApplicationService 认证服务
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AdminAuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 管理员账号密码登录。
     *
     * @param dto 请求
     * @return 登录结果
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Operation(summary = "管理员登录")
    @SecurityRequirements
    @PostMapping("/login")
    public ApiResult<LoginVo> login(@Valid @RequestBody AdminLoginDto dto) {
        return ApiResult.ok(authApplicationService.adminLogin(dto));
    }

    /**
     * 当前管理员资料（校验 Token）。
     *
     * @return 主体信息
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Operation(summary = "当前管理员信息")
    @GetMapping("/me")
    public ApiResult<LoginVo> me() {
        AuthPrincipal principal = SecurityUtils.requireAdmin();
        LoginVo vo = new LoginVo()
                .setUserId(principal.getId())
                .setDisplayName(principal.getUsername())
                .setPrincipalType(principal.getType().name())
                .setPermissions(principal.getPermissions());
        return ApiResult.ok(vo);
    }
}
