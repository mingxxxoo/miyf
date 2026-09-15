package cn.miyf.kitchen.controller.admin;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.DishAuditRejectDto;
import cn.miyf.kitchen.bean.qo.BindingPageQo;
import cn.miyf.kitchen.bean.qo.KitchenPageQo;
import cn.miyf.kitchen.bean.vo.DishAuditTaskVo;
import cn.miyf.kitchen.bean.vo.KitchenBindingVo;
import cn.miyf.kitchen.bean.vo.KitchenVo;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import cn.miyf.kitchen.service.BindingApplicationService;
import cn.miyf.kitchen.service.DishAuditWorkflowService;
import cn.miyf.kitchen.service.KitchenApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：厨房、绑定、菜品审核待办。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Tag(name = "管理端胡闹厨房")
@KitchenAdminPopedom
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminKitchenPlatformController {

    private final KitchenApplicationService kitchenApplicationService;
    private final BindingApplicationService bindingApplicationService;
    private final DishAuditWorkflowService dishAuditWorkflowService;

    /**
     * 厨房分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨房分页")
    @MiyfPermission(code = "kitchen:kitchen:list")
    @GetMapping("/kitchens")
    public ApiResult<PageResult<KitchenVo>> kitchens(KitchenPageQo qo) {
        return ApiResult.ok(kitchenApplicationService.pageAdmin(qo));
    }

    /**
     * 更新厨房状态（含封禁）。
     *
     * @param id     厨房 ID
     * @param status OPEN/CLOSED/BANNED
     * @return 更新后厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "更新厨房状态")
    @MiyfPermission(code = "kitchen:kitchen:list")
    @PutMapping("/kitchens/{id}/status")
    public ApiResult<KitchenVo> kitchenStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResult.ok(kitchenApplicationService.updateAdminStatus(id, status));
    }

    /**
     * 绑定关系分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "绑定分页")
    @MiyfPermission(code = "kitchen:binding:list")
    @GetMapping("/bindings")
    public ApiResult<PageResult<KitchenBindingVo>> bindings(BindingPageQo qo) {
        return ApiResult.ok(bindingApplicationService.pageAdmin(qo));
    }

    /**
     * 强制解除绑定。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "强制解除绑定")
    @MiyfPermission(code = "kitchen:binding:list")
    @PostMapping("/bindings/{id}/unbind")
    public ApiResult<KitchenBindingVo> unbind(@PathVariable Long id) {
        return ApiResult.ok(bindingApplicationService.unbindAdmin(id));
    }

    /**
     * 菜品审核待办列表。
     *
     * @param limit 条数（默认 50，最大 100）
     * @return 待办列表
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "菜品审核待办")
    @MiyfPermission(code = "kitchen:dish:audit")
    @GetMapping("/dish-audits")
    public ApiResult<java.util.List<DishAuditTaskVo>> audits(@RequestParam(required = false) Integer limit) {
        int max = limit == null ? 50 : Math.min(Math.max(limit, 1), 100);
        return ApiResult.ok(dishAuditWorkflowService.listPending(0, max));
    }

    /**
     * 通过菜品审核。
     *
     * @param taskId Flowable 任务 ID
     * @return 空
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "通过菜品审核")
    @MiyfPermission(code = "kitchen:dish:audit")
    @PostMapping("/dish-audits/{taskId}/approve")
    public ApiResult<Void> approve(@PathVariable String taskId) {
        dishAuditWorkflowService.approve(taskId);
        return ApiResult.ok();
    }

    /**
     * 驳回菜品审核。
     *
     * @param taskId Flowable 任务 ID
     * @param dto    驳回原因
     * @return 空
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "驳回菜品审核")
    @MiyfPermission(code = "kitchen:dish:audit")
    @PostMapping("/dish-audits/{taskId}/reject")
    public ApiResult<Void> reject(@PathVariable String taskId, @Valid @RequestBody DishAuditRejectDto dto) {
        dishAuditWorkflowService.reject(taskId, dto.getReason());
        return ApiResult.ok();
    }
}
