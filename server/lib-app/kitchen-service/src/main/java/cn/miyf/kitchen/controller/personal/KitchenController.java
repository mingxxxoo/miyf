package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.KitchenSaveDto;
import cn.miyf.kitchen.bean.vo.KitchenInviteVo;
import cn.miyf.kitchen.bean.vo.KitchenVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.InviteApplicationService;
import cn.miyf.kitchen.service.KitchenApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 厨师厨房与邀请。
 * 一用户一厨；邀请码用于食客申请绑定。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Tag(name = "用户端厨房")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/my-kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenApplicationService kitchenApplicationService;
    private final InviteApplicationService inviteApplicationService;

    /**
     * 当前厨师厨房。
     *
     * @return 厨房 VO，未创建时 data 为 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "我的厨房")
    @MiyfPermission(code = "kitchen:user:profile:view")
    @GetMapping
    public ApiResult<KitchenVo> mine() {
        return ApiResult.ok(kitchenApplicationService.getMine());
    }

    /**
     * 创建或更新厨房资料。
     *
     * @param dto 厨房内容
     * @return 保存后厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "创建或更新厨房")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PutMapping
    public ApiResult<KitchenVo> save(@Valid @RequestBody KitchenSaveDto dto) {
        return ApiResult.ok(kitchenApplicationService.saveMine(dto));
    }

    /**
     * 当前有效邀请码。
     *
     * @return 邀请 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "当前邀请码")
    @MiyfPermission(code = "kitchen:user:profile:view")
    @GetMapping("/invite")
    public ApiResult<KitchenInviteVo> invite() {
        return ApiResult.ok(inviteApplicationService.getMine());
    }

    /**
     * 更换邀请码（旧码立即失效）。
     *
     * @return 新邀请 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "更换邀请码")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/invite/rotate")
    public ApiResult<KitchenInviteVo> rotate() {
        return ApiResult.ok(inviteApplicationService.rotateMine());
    }
}
