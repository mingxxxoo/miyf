package cn.miyf.kitchen.service;

import cn.miyf.auth.service.WxSubscribeMessageService;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.SystemConfigReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 厨师推进预约状态后向食客推送微信订阅消息。
 * 失败不影响状态流转；须食客曾在小程序授权对应模板。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDinerWxNotifyService {

    private static final String CFG_ENABLED = "wx.subscribe.enabled";
    private static final String CFG_TEMPLATE_ID = "wx.subscribe.order_status.template_id";
    private static final String CFG_PAGE = "wx.subscribe.order_status.page";
    private static final String CFG_MINIPROGRAM_STATE = "wx.subscribe.miniprogram_state";

    private static final String DEFAULT_PAGE = "pages/order/index";
    private static final String DEFAULT_MINIPROGRAM_STATE = "formal";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final WxSubscribeMessageService wxSubscribeMessageService;
    private final SystemConfigReader systemConfigReader;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;

    /**
     * 预约状态变更通知食客（尽力而为）。
     *
     * @param order 已更新后的预约（含明细）
     */
    public void notifyStatusChanged(Order order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        String templateId = resolveTemplateId();
        if (!StringUtils.hasText(templateId)) {
            log.debug("skip diner wx notify: template_id blank");
            return;
        }
        try {
            UserEntity diner = userRepository.selectById(order.getUserId());
            if (diner == null || !StringUtils.hasText(diner.getOpenid())) {
                log.warn("skip diner wx notify: openid missing userId={}", order.getUserId());
                return;
            }
            Map<String, String> data = buildData(order);
            boolean ok = wxSubscribeMessageService.send(
                    diner.getOpenid(), templateId, resolvePage(), resolveMiniprogramState(), data);
            if (ok) {
                log.info("diner wx subscribe sent orderNo={} status={}", order.getOrderNo(), order.getStatus());
            }
        } catch (Exception ex) {
            log.warn("diner wx notify failed orderNo={}: {}", order.getOrderNo(), ex.toString());
        }
    }

    public String resolveTemplateId() {
        String fromDb = systemConfigReader.getRaw(CFG_TEMPLATE_ID);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : "";
    }

    public boolean isEnabled() {
        return systemConfigReader.getBoolean(CFG_ENABLED, false);
    }

    public String resolvePage() {
        String fromDb = systemConfigReader.getRaw(CFG_PAGE);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : DEFAULT_PAGE;
    }

    public String resolveMiniprogramState() {
        String fromDb = systemConfigReader.getRaw(CFG_MINIPROGRAM_STATE);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : DEFAULT_MINIPROGRAM_STATE;
    }

    private Map<String, String> buildData(Order order) {
        String kitchenName = resolveKitchenName(order.getKitchenId());
        String remarkOrKitchen = StringUtils.hasText(order.getRemark()) ? order.getRemark() : kitchenName;
        Map<String, String> data = new LinkedHashMap<>();
        data.put("thing1", statusLabel(order.getStatus()));
        data.put("character_string2", order.getOrderNo() == null ? "" : order.getOrderNo());
        data.put("thing3", summarizeDishes(order));
        data.put("time4", TIME_FMT.format(Instant.now()));
        data.put("thing5", truncate(remarkOrKitchen, 20));
        return data;
    }

    private String resolveKitchenName(Long kitchenId) {
        if (kitchenId == null) {
            return "厨房";
        }
        KitchenEntity kitchen = kitchenRepository.selectById(kitchenId);
        if (kitchen != null && StringUtils.hasText(kitchen.getName())) {
            return kitchen.getName();
        }
        return "厨房";
    }

    private static String statusLabel(String status) {
        if (status == null) {
            return "有更新";
        }
        return switch (status) {
            case "PENDING" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "PREPARING" -> "备餐中";
            case "READY" -> "可取餐";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> "有更新";
        };
    }

    private String summarizeDishes(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "预约有更新";
        }
        String joined = order.getItems().stream()
                .map(OrderItem::getDishName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return truncate(StringUtils.hasText(joined) ? joined : "预约有更新", 20);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
