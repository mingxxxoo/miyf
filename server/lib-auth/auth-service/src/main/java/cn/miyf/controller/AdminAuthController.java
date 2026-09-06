package cn.miyf.controller;

import cn.miyf.bean.dto.AdminLoginDto;
import cn.miyf.bean.vo.CaptchaVo;
import cn.miyf.bean.vo.LoginRiskVo;
import cn.miyf.bean.vo.LoginVo;
import cn.miyf.common.ApiResult;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.SecurityUtils;
import cn.miyf.service.AuthApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * 查询登录风控状态（是否需验证码 / 是否锁定）。
     *
     * @param username 用户名
     * @return 风控状态
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "登录风控状态")
    @SecurityRequirements
    @GetMapping("/login-status")
    public ApiResult<LoginRiskVo> loginStatus(@RequestParam String username) {
        return ApiResult.ok(authApplicationService.loginStatus(username));
    }

    /**
     * 获取图形验证码。
     *
     * @return 验证码
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "获取图形验证码")
    @SecurityRequirements
    @GetMapping("/captcha")
    public ApiResult<CaptchaVo> captcha() {
        return ApiResult.ok(authApplicationService.createCaptcha());
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
