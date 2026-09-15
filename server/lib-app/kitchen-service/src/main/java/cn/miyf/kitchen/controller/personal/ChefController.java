package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.dto.OrderStatusUpdateDto;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.DishApplicationService;
import cn.miyf.kitchen.service.OrderApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 厨师菜品与接单。
 * 菜品须审核通过后方可上架；预约状态由厨师侧流转。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Tag(name = "厨师工作台")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/chef")
@RequiredArgsConstructor
public class ChefController {

    private final DishApplicationService dishApplicationService;
    private final OrderApplicationService orderApplicationService;

    /**
     * 本厨房菜品分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨师菜品分页")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @GetMapping("/dishes")
    public ApiResult<PageResult<DishVo>> dishes(DishPageQo qo) {
        return ApiResult.ok(dishApplicationService.pageChef(qo));
    }

    /**
     * 本厨房菜品详情。
     *
     * @param id 菜品 ID
     * @return 菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨师菜品详情")
    @MiyfPermission(code = "kitchen:user:dish:detail")
    @GetMapping("/dishes/{id}")
    public ApiResult<DishVo> dish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.getChefDetail(id));
    }

    /**
     * 创建菜品草稿。
     *
     * @param dto 菜品内容
     * @return 新建菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "创建菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes")
    public ApiResult<DishVo> createDish(@Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.createChef(dto));
    }

    /**
     * 更新菜品；审核中不可改，关键字段变更会回退审核状态。
     *
     * @param id  菜品 ID
     * @param dto 菜品内容
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "更新菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PutMapping("/dishes/{id}")
    public ApiResult<DishVo> updateDish(@PathVariable Long id, @Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.updateChef(id, dto));
    }

    /**
     * 提交菜品审核。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "提交菜品审核")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/submit-audit")
    public ApiResult<DishVo> submit(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.submitChefAudit(id));
    }

    /**
     * 撤回审核中的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "撤回菜品审核")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/withdraw-audit")
    public ApiResult<DishVo> withdraw(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.withdrawChefAudit(id));
    }

    /**
     * 上架已审核通过的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "上架菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/publish")
    public ApiResult<DishVo> publish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.publishChef(id));
    }

    /**
     * 下架本厨房上架菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "下架菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/unpublish")
    public ApiResult<DishVo> unpublish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.unpublishChef(id));
    }

    /**
     * 本厨房预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨房预约分页")
    @MiyfPermission(code = "kitchen:user:order:list")
    @GetMapping("/orders")
    public ApiResult<PageResult<OrderVo>> orders(OrderPageQo qo) {
        return ApiResult.ok(orderApplicationService.pageChef(qo));
    }

    /**
     * 本厨房预约详情。
     *
     * @param id 预约 ID
     * @return 预约
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨房预约详情")
    @MiyfPermission(code = "kitchen:user:order:detail")
    @GetMapping("/orders/{id}")
    public ApiResult<OrderVo> order(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.getChef(id));
    }

    /**
     * 厨师处理预约状态。
     *
     * @param id  预约 ID
     * @param dto 目标状态
     * @return 更新后预约
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "处理预约状态")
    @MiyfPermission(code = "kitchen:user:order:list")
    @PutMapping("/orders/{id}/status")
    public ApiResult<OrderVo> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateDto dto) {
        return ApiResult.ok(orderApplicationService.updateStatusChef(id, dto));
    }
}
