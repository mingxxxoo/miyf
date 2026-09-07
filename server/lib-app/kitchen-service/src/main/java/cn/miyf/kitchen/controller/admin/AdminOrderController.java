package cn.miyf.kitchen.controller.admin;

import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.OrderStatusUpdateDto;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.service.OrderApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 管理端预约接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Tag(name = "管理端预约")
@KitchenAdminPopedom
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderApplicationService orderApplicationService;

    /**
     * 构造控制器。
     *
     * @param orderApplicationService 预约服务
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public AdminOrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    /**
     * 预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "预约分页")
    @MiyfPermission(code = "kitchen:order:list")
    @GetMapping
    public ApiResult<PageResult<OrderVo>> page(OrderPageQo qo) {
        return ApiResult.ok(orderApplicationService.pageAdmin(qo));
    }

    /**
     * 预约详情。
     *
     * @param id 预约 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "预约详情")
    @MiyfPermission(code = "kitchen:order:detail")
    @RequirePermission({"kitchen:order:detail", "kitchen:order:list"})
    @GetMapping("/{id}")
    public ApiResult<OrderVo> detail(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.getAdmin(id));
    }

    /**
     * 更新预约状态（状态机约束；取消回补库存）。
     *
     * @param id  预约 ID
     * @param dto 目标状态
     * @return 更新后预约
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "更新预约状态")
    @MiyfPermission(code = "kitchen:order:update")
    @MiyfPermission(code = "kitchen:order:cancel")
    @MiyfPermission(code = "kitchen:order:complete")
    @RequirePermission({"kitchen:order:update", "kitchen:order:cancel", "kitchen:order:complete"})
    @PutMapping("/{id}/status")
    public ApiResult<OrderVo> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody OrderStatusUpdateDto dto) {
        return ApiResult.ok(orderApplicationService.updateStatusAdmin(id, dto));
    }
}
