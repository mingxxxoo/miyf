package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.OrderCreateDto;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.bean.vo.WxSubscribeConfigVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.OrderApplicationService;
import cn.miyf.kitchen.service.OrderDinerWxNotifyService;
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

import java.util.List;

/**
 * 用户端预约接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Tag(name = "用户端预约")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;
    private final OrderDinerWxNotifyService orderDinerWxNotifyService;

    /**
     * 微信订阅消息配置（食客端：预约状态变更提醒）。
     *
     * @return 开关与模板 ID
     */
    @Operation(summary = "预约状态订阅消息配置")
    @MiyfPermission(code = "kitchen:user:order:create")
    @GetMapping("/wx-subscribe-config")
    public ApiResult<WxSubscribeConfigVo> wxSubscribeConfig() {
        String templateId = orderDinerWxNotifyService.resolveTemplateId();
        boolean enabled = orderDinerWxNotifyService.isEnabled()
                && templateId != null && !templateId.isBlank();
        return ApiResult.ok(new WxSubscribeConfigVo()
                .setEnabled(enabled)
                .setTemplateIds(enabled ? List.of(templateId.trim()) : List.of()));
    }

    /**
     * 创建预约。
     *
     * @param dto 请求
     * @return 预约结果
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "创建预约")
    @MiyfPermission(code = "kitchen:user:order:create")
    @PostMapping
    public ApiResult<OrderVo> create(@Valid @RequestBody OrderCreateDto dto) {
        return ApiResult.ok(orderApplicationService.create(dto));
    }

    /**
     * 我的预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "我的预约列表")
    @MiyfPermission(code = "kitchen:user:order:list")
    @GetMapping
    public ApiResult<PageResult<OrderVo>> page(OrderPageQo qo) {
        return ApiResult.ok(orderApplicationService.pageMine(qo));
    }

    /**
     * 我的预约详情。
     *
     * @param id 预约 ID
     * @return 详情
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "我的预约详情")
    @MiyfPermission(code = "kitchen:user:order:detail")
    @GetMapping("/{id}")
    public ApiResult<OrderVo> detail(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.getMine(id));
    }

    /**
     * 取消预约（仅 PENDING）。
     *
     * @param id 预约 ID
     * @return 取消后预约
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "取消预约")
    @MiyfPermission(code = "kitchen:user:order:cancel")
    @PostMapping("/{id}/cancel")
    public ApiResult<OrderVo> cancel(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.cancelMine(id));
    }
}
