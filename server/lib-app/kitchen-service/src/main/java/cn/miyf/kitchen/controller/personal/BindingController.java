package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.BindingApplyDto;
import cn.miyf.kitchen.bean.dto.BindingRejectDto;
import cn.miyf.kitchen.bean.qo.BindingPageQo;
import cn.miyf.kitchen.bean.vo.KitchenBindingVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.BindingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 绑定申请与确认。
 * 一客一厨；食客申请、厨师通过/拒绝、双方可解绑。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Tag(name = "用户端绑定")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final BindingApplicationService bindingApplicationService;

    /**
     * 当前食客绑定关系。
     *
     * @return 绑定 VO，无则 data 为 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "当前绑定")
    @MiyfPermission(code = "kitchen:user:profile:view")
    @GetMapping("/mine")
    public ApiResult<KitchenBindingVo> mine() {
        return ApiResult.ok(bindingApplicationService.getMine());
    }

    /**
     * 凭邀请申请加入厨房。
     *
     * @param dto 邀请码或 token
     * @return 绑定 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "申请加入厨房")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/apply")
    public ApiResult<KitchenBindingVo> apply(@Valid @RequestBody BindingApplyDto dto) {
        return ApiResult.ok(bindingApplicationService.apply(dto));
    }

    /**
     * 厨师端本厨房绑定分页。
     *
     * @param qo 状态与分页
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨师绑定列表")
    @MiyfPermission(code = "kitchen:user:profile:view")
    @GetMapping
    public ApiResult<PageResult<KitchenBindingVo>> chefPage(BindingPageQo qo) {
        return ApiResult.ok(bindingApplicationService.pageChef(qo));
    }

    /**
     * 厨师通过绑定申请。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "通过绑定")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/{id}/approve")
    public ApiResult<KitchenBindingVo> approve(@PathVariable Long id) {
        return ApiResult.ok(bindingApplicationService.approve(id));
    }

    /**
     * 厨师拒绝绑定申请。
     *
     * @param id  绑定 ID
     * @param dto 拒绝原因（可空）
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "拒绝绑定")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/{id}/reject")
    public ApiResult<KitchenBindingVo> reject(@PathVariable Long id, @RequestBody(required = false) BindingRejectDto dto) {
        return ApiResult.ok(bindingApplicationService.reject(id, dto));
    }

    /**
     * 厨师或食客解除绑定。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "解除绑定")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PostMapping("/{id}/unbind")
    public ApiResult<KitchenBindingVo> unbind(@PathVariable Long id) {
        return ApiResult.ok(bindingApplicationService.unbind(id));
    }
}
