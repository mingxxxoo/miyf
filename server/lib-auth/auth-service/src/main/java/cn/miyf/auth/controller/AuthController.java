package cn.miyf.auth.controller;

import cn.miyf.auth.bean.dto.WxLoginDto;
import cn.miyf.auth.bean.vo.LoginVo;
import cn.miyf.auth.service.WxLoginService;
import cn.miyf.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证接口（微信登录等）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Tag(name = "用户认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WxLoginService wxLoginService;

    /**
     * 微信登录。
     *
     * @param dto 请求
     * @return 登录结果
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "微信小程序登录（须提交用户名、手机号、微信号）")
    @SecurityRequirements
    @PostMapping("/wx-login")
    public ApiResult<LoginVo> wxLogin(@Valid @RequestBody WxLoginDto dto) {
        return ApiResult.ok(wxLoginService.wxLogin(dto));
    }
}
