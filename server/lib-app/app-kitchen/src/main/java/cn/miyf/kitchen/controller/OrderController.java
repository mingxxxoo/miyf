package cn.miyf.kitchen.controller;

import cn.miyf.kitchen.service.OrderApplicationService;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.OrderCreateDto;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户端预约接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Tag(name = "用户端预约")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    /**
     * 构造控制器。
     *
     * @param orderApplicationService 预约服务
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    /**
     * 创建预约。
     *
     * @param dto 请求
     * @return 预约结果
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "创建预约")
    @PostMapping
    public ApiResult<OrderVo> create(@Valid @RequestBody OrderCreateDto dto) {
        return ApiResult.ok(orderApplicationService.create(dto));
    }

    /**
     * 我的预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "我的预约列表")
    @GetMapping
    public ApiResult<PageResult<OrderVo>> page(OrderPageQo qo) {
        return ApiResult.ok(orderApplicationService.pageMine(qo));
    }

    /**
     * 我的预约详情。
     *
     * @param id 预约 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "我的预约详情")
    @GetMapping("/{id}")
    public ApiResult<OrderVo> detail(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.getMine(id));
    }

    /**
     * 取消预约（仅 PENDING）。
     *
     * @param id 预约 ID
     * @return 取消后预约
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Operation(summary = "取消预约")
    @PostMapping("/{id}/cancel")
    public ApiResult<OrderVo> cancel(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.cancelMine(id));
    }
}
