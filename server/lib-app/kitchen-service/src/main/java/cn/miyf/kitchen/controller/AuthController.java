package cn.miyf.kitchen.controller;

import cn.miyf.bean.dto.WxLoginDto;
import cn.miyf.bean.vo.LoginVo;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.service.KitchenAuthApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KitchenAuthApplicationService kitchenAuthApplicationService;

    /**
     * 构造控制器。
     *
     * @param kitchenAuthApplicationService 厨房认证服务
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthController(KitchenAuthApplicationService kitchenAuthApplicationService) {
        this.kitchenAuthApplicationService = kitchenAuthApplicationService;
    }

    /**
     * 微信登录。
     *
     * @param dto 请求
     * @return 登录结果
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Operation(summary = "微信小程序登录")
    @SecurityRequirements
    @PostMapping("/wx-login")
    public ApiResult<LoginVo> wxLogin(@Valid @RequestBody WxLoginDto dto) {
        return ApiResult.ok(kitchenAuthApplicationService.wxLogin(dto));
    }
}
