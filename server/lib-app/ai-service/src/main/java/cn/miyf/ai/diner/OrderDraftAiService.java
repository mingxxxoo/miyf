package cn.miyf.ai.diner;

import cn.miyf.ai.bean.dto.OrderDraftAiDto;
import cn.miyf.ai.bean.model.OrderDraftAiResult;
import cn.miyf.ai.bean.vo.OrderDraftAiVo;
import cn.miyf.ai.config.AiProperties;
import cn.miyf.ai.prompt.AiPromptTemplates;
import cn.miyf.ai.support.AiStructuredCaller;
import cn.miyf.ai.support.MenuContextBuilder;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.DishEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 场景二调用①：自然语言 → 预约单草稿（不提交订单）。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class OrderDraftAiService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String[] WEEKDAYS = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    private final AiStructuredCaller aiStructuredCaller;
    private final AiProperties aiProperties;
    private final MenuContextBuilder menuContextBuilder;

    /**
     * 生成预约草稿。
     *
     * @param dto 食客文本
     * @return 草稿 VO
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public OrderDraftAiVo draft(OrderDraftAiDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getText())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入想吃什么");
        }
        String dinerText = dto.getText().trim();
        MenuContextBuilder.MenuContext menu = menuContextBuilder.buildForBoundKitchen();
        LocalDate today = LocalDate.now();
        String weekday = WEEKDAYS[today.getDayOfWeek().getValue() - 1];

        if (!aiStructuredCaller.isAvailable()) {
            return emptyDraft(dinerText, true);
        }

        String system = AiPromptTemplates.ORDER_DRAFT_SYSTEM.replace("{menuJson}", menu.menuJson());
        String user = AiPromptTemplates.orderDraftUser(today.format(DATE_FMT), weekday, dinerText);
        try {
            OrderDraftAiResult result = aiStructuredCaller.callEntity(
                    "ORDER_DRAFT",
                    system,
                    user,
                    aiProperties.getOrderDraft().getTemperature(),
                    aiProperties.getOrderDraft().getRetryOnTransient(),
                    OrderDraftAiResult.class,
                    SecurityUtils.currentUserId(),
                    menu.kitchenId(),
                    summarize(dinerText));
            return sanitize(result, menu, dinerText);
        } catch (Exception ex) {
            return emptyDraft(dinerText, true);
        }
    }

    private OrderDraftAiVo emptyDraft(String dinerText, boolean degraded) {
        OrderDraftAiVo.UnmatchedVo unmatched = new OrderDraftAiVo.UnmatchedVo()
                .setRawText(dinerText)
                .setReason(degraded ? "AI 暂不可用，请手动选菜" : "没有识别出菜品需求");
        return new OrderDraftAiVo()
                .setDegraded(degraded)
                .setItems(List.of())
                .setUnmatched(List.of(unmatched));
    }

    private OrderDraftAiVo sanitize(OrderDraftAiResult result,
                                    MenuContextBuilder.MenuContext menu,
                                    String dinerText) {
        if (result == null) {
            return emptyDraft(dinerText, false);
        }
        Set<String> allowed = menu.dishIds();
        List<OrderDraftAiVo.ItemVo> items = new ArrayList<>();
        List<OrderDraftAiVo.UnmatchedVo> unmatched = new ArrayList<>();
        if (result.getUnmatched() != null) {
            for (OrderDraftAiResult.UnmatchedItem u : result.getUnmatched()) {
                unmatched.add(new OrderDraftAiVo.UnmatchedVo().setRawText(u.getRawText()).setReason(u.getReason()));
            }
        }
        if (result.getItems() != null) {
            for (OrderDraftAiResult.DraftItem item : result.getItems()) {
                if (item == null || !StringUtils.hasText(item.getDishId())) {
                    continue;
                }
                String dishId = item.getDishId().trim();
                if (!allowed.contains(dishId)) {
                    unmatched.add(new OrderDraftAiVo.UnmatchedVo()
                            .setRawText(item.getDishName() == null ? dishId : item.getDishName())
                            .setReason("菜单中不存在该菜品，已剔除"));
                    continue;
                }
                DishEntity dish = menu.dishesById().get(dishId);
                int qty = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : Math.min(item.getQuantity(), 5);
                items.add(new OrderDraftAiVo.ItemVo()
                        .setDishId(dishId)
                        .setDishName(dish != null ? dish.getName() : item.getDishName())
                        .setQuantity(qty)
                        .setConfidence(normalizeConfidence(item.getConfidence()))
                        .setItemNote(item.getItemNote()));
            }
        }
        return new OrderDraftAiVo()
                .setDegraded(false)
                .setItems(items)
                .setMealDate(normalizeDate(result.getMealDate()))
                .setMealType(normalizeMealType(result.getMealType()))
                .setGuestCount(result.getGuestCount())
                .setNote(result.getNote())
                .setUnmatched(unmatched)
                .setAmbiguityNote(result.getAmbiguityNote());
    }

    private String normalizeConfidence(String confidence) {
        if (!StringUtils.hasText(confidence)) {
            return "medium";
        }
        String c = confidence.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "high", "medium", "low" -> c;
            default -> "medium";
        };
    }

    private String normalizeMealType(String mealType) {
        if (!StringUtils.hasText(mealType)) {
            return null;
        }
        String upper = mealType.trim().toUpperCase(Locale.ROOT);
        return ("LUNCH".equals(upper) || "DINNER".equals(upper)) ? upper : null;
    }

    private String normalizeDate(String mealDate) {
        if (!StringUtils.hasText(mealDate)) {
            return null;
        }
        try {
            return LocalDate.parse(mealDate.trim(), DATE_FMT).format(DATE_FMT);
        } catch (Exception ex) {
            return null;
        }
    }

    private String summarize(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) : oneLine;
    }
}
