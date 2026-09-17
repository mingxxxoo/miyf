package cn.miyf.ai.diner;

import cn.miyf.ai.bean.dto.OrderInsightAiDto;
import cn.miyf.ai.bean.model.OrderInsightAiResult;
import cn.miyf.ai.bean.vo.OrderInsightAiVo;
import cn.miyf.ai.config.AiProperties;
import cn.miyf.ai.prompt.AiPromptTemplates;
import cn.miyf.ai.support.AiStructuredCaller;
import cn.miyf.ai.support.HealthContextBuilder;
import cn.miyf.ai.support.MenuContextBuilder;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 场景二调用②：预约草稿 + 异常指标 → 饮食参考分析（生活方式参考，非诊断）。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class OrderInsightAiService {

    private final AiStructuredCaller aiStructuredCaller;
    private final AiProperties aiProperties;
    private final MenuContextBuilder menuContextBuilder;
    private final HealthContextBuilder healthContextBuilder;

    /**
     * 分析预约草稿与近期异常指标的匹配度。
     *
     * @param dto 草稿与诉求
     * @return 分析 VO；失败时 degraded
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public OrderInsightAiVo analyze(OrderInsightAiDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getDraftJson())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少预约草稿");
        }
        String draftJson = dto.getDraftJson().trim();
        if ("{}".equals(draftJson) || "[]".equals(draftJson) || draftJson.contains("\"items\":[]")) {
            // 空草稿不触发分析
            return unavailable("草稿为空，跳过分析");
        }
        if (!aiStructuredCaller.isAvailable()) {
            return unavailable("分析暂不可用");
        }

        MenuContextBuilder.MenuContext menu = menuContextBuilder.buildForBoundKitchen();
        // 健康数据最小化：只注入异常指标；失败则 []
        String healthAlertsJson = healthContextBuilder.buildAlertsJson();
        String dinerText = dto.getDinerText() == null ? "" : dto.getDinerText().trim();
        String user = AiPromptTemplates.orderInsightUser(draftJson, healthAlertsJson, dinerText, menu.menuJson());

        try {
            OrderInsightAiResult result = aiStructuredCaller.callEntity(
                    "ORDER_INSIGHT",
                    AiPromptTemplates.ORDER_INSIGHT_SYSTEM,
                    user,
                    aiProperties.getOrderInsight().getTemperature(),
                    aiProperties.getOrderInsight().getRetryOnTransient(),
                    OrderInsightAiResult.class,
                    SecurityUtils.currentUserId(),
                    menu.kitchenId(),
                    "insight:" + summarize(dinerText));
            return toVo(result, menu.dishIds());
        } catch (Exception ex) {
            return unavailable("分析暂不可用");
        }
    }

    private OrderInsightAiVo unavailable(String summary) {
        return new OrderInsightAiVo()
                .setDegraded(true)
                .setOverallVerdict("OK")
                .setSummary(summary)
                .setDishInsights(List.of())
                .setSuggestions(List.of())
                .setDisclaimer(OrderInsightAiResult.DEFAULT_DISCLAIMER);
    }

    private OrderInsightAiVo toVo(OrderInsightAiResult result, Set<String> allowedDishIds) {
        if (result == null) {
            return unavailable("分析暂不可用");
        }
        OrderInsightAiVo vo = new OrderInsightAiVo()
                .setDegraded(false)
                .setOverallVerdict(normalizeVerdict(result.getOverallVerdict()))
                .setSummary(StringUtils.hasText(result.getSummary()) ? result.getSummary() : "暂无额外建议")
                .setDisclaimer(OrderInsightAiResult.DEFAULT_DISCLAIMER);

        List<OrderInsightAiVo.DishInsightVo> insights = new ArrayList<>();
        if (result.getDishInsights() != null) {
            for (OrderInsightAiResult.DishInsight d : result.getDishInsights()) {
                if (d == null) {
                    continue;
                }
                insights.add(new OrderInsightAiVo.DishInsightVo()
                        .setDishId(d.getDishId())
                        .setDishName(d.getDishName())
                        .setVerdict(normalizeVerdict(d.getVerdict()))
                        .setReason(d.getReason()));
            }
        }
        vo.setDishInsights(insights);

        List<OrderInsightAiVo.SuggestionVo> suggestions = new ArrayList<>();
        if (result.getSuggestions() != null) {
            for (OrderInsightAiResult.Suggestion s : result.getSuggestions()) {
                if (s == null || !StringUtils.hasText(s.getText())) {
                    continue;
                }
                String replaceId = s.getReplaceDishId();
                if (StringUtils.hasText(replaceId) && !allowedDishIds.contains(replaceId.trim())) {
                    // 替代菜必须来自菜单
                    replaceId = null;
                }
                suggestions.add(new OrderInsightAiVo.SuggestionVo()
                        .setType(normalizeSuggestionType(s.getType()))
                        .setText(s.getText())
                        .setReplaceDishId(replaceId)
                        .setReplaceDishName(replaceId == null ? null : s.getReplaceDishName()));
            }
        }
        vo.setSuggestions(suggestions);
        return vo;
    }

    private String normalizeVerdict(String verdict) {
        if (!StringUtils.hasText(verdict)) {
            return "OK";
        }
        String upper = verdict.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "OK", "CAUTION", "AVOID" -> upper;
            default -> "OK";
        };
    }

    private String normalizeSuggestionType(String type) {
        if (!StringUtils.hasText(type)) {
            return "GENERAL";
        }
        String upper = type.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "REPLACE", "REMARK", "GENERAL" -> upper;
            default -> "GENERAL";
        };
    }

    private String summarize(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 80 ? oneLine.substring(0, 80) : oneLine;
    }
}
