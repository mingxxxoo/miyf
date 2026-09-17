package cn.miyf.ai.controller.personal;

import cn.miyf.ai.bean.dto.OrderDraftAiDto;
import cn.miyf.ai.bean.dto.OrderInsightAiDto;
import cn.miyf.ai.bean.vo.OrderDraftAiVo;
import cn.miyf.ai.bean.vo.OrderInsightAiVo;
import cn.miyf.ai.diner.OrderDraftAiService;
import cn.miyf.ai.diner.OrderInsightAiService;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 食客端 AI 预约草稿与饮食参考。
 * 草稿绝不自动下单；分析失败静默降级。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Tag(name = "食客 AI")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/ai/orders")
@RequiredArgsConstructor
public class DinerAiController {

    private final OrderDraftAiService orderDraftAiService;
    private final OrderInsightAiService orderInsightAiService;

    /**
     * 自然语言整理预约草稿。
     *
     * @param dto 食客文本
     * @return 草稿
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Operation(summary = "AI 预约草稿")
    @MiyfPermission(code = "kitchen:user:order:create")
    @PostMapping("/draft")
    public ApiResult<OrderDraftAiVo> draft(@Valid @RequestBody OrderDraftAiDto dto) {
        return ApiResult.ok(orderDraftAiService.draft(dto));
    }

    /**
     * 指标满足度饮食参考分析。
     *
     * @param dto 草稿与诉求
     * @return 分析卡
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Operation(summary = "AI 饮食参考分析")
    @MiyfPermission(code = "kitchen:user:order:create")
    @PostMapping("/insight")
    public ApiResult<OrderInsightAiVo> insight(@Valid @RequestBody OrderInsightAiDto dto) {
        return ApiResult.ok(orderInsightAiService.analyze(dto));
    }
}
