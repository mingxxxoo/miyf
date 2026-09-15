package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.RoleChooseDto;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份选择与工作台切换。
 * POST 开通身份；PUT 在已开通身份间切换 activeRole。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Tag(name = "用户端身份")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class RoleController {

    private final UserApplicationService userApplicationService;

    /**
     * 首次选择并开通厨师或食客身份。
     *
     * @param dto CHEF 或 DINER
     * @return 用户 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "选择身份")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/me/role")
    public ApiResult<UserVo> choose(@Valid @RequestBody RoleChooseDto dto) {
        return ApiResult.ok(userApplicationService.chooseRole(SecurityUtils.currentUserId(), dto.getRole()));
    }

    /**
     * 切换当前工作台身份（须已开通）。
     *
     * @param dto CHEF 或 DINER
     * @return 用户 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "切换工作台")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PutMapping("/me/role")
    public ApiResult<UserVo> switchRole(@Valid @RequestBody RoleChooseDto dto) {
        return ApiResult.ok(userApplicationService.switchActiveRole(SecurityUtils.currentUserId(), dto.getRole()));
    }
}
