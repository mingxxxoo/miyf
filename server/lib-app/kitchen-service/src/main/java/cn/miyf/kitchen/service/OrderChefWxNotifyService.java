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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 食客下单后向厨师推送微信订阅消息。
 * 失败不影响下单主流程；须厨师曾在小程序授权对应模板。
 * 开关、模板 ID、跳转页、小程序版本态均从系统配置（基础设置）热读。
 *
 * @author XieMingJie
 * @since 2026-09-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderChefWxNotifyService {

    private static final String CFG_ENABLED = "wx.subscribe.enabled";
    private static final String CFG_TEMPLATE_ID = "wx.subscribe.new_order.template_id";
    private static final String CFG_PAGE = "wx.subscribe.new_order.page";
    private static final String CFG_MINIPROGRAM_STATE = "wx.subscribe.miniprogram_state";

    private static final String DEFAULT_PAGE = "pages/chef/orders";
    private static final String DEFAULT_MINIPROGRAM_STATE = "formal";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final WxSubscribeMessageService wxSubscribeMessageService;
    private final SystemConfigReader systemConfigReader;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;

    /**
     * 新预约通知厨师（尽力而为）。
     *
     * @param order 已落库预约（含明细）
     */
    public void notifyNewOrder(Order order) {
        if (order == null || order.getKitchenId() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        String templateId = resolveTemplateId();
        if (!StringUtils.hasText(templateId)) {
            log.debug("skip chef wx notify: template_id blank");
            return;
        }
        try {
            KitchenEntity kitchen = kitchenRepository.selectById(order.getKitchenId());
            if (kitchen == null || kitchen.getOwnerUserId() == null) {
                log.warn("skip chef wx notify: kitchen missing id={}", order.getKitchenId());
                return;
            }
            UserEntity chef = userRepository.selectById(kitchen.getOwnerUserId());
            if (chef == null || !StringUtils.hasText(chef.getOpenid())) {
                log.warn("skip chef wx notify: chef openid missing kitchenId={}", order.getKitchenId());
                return;
            }
            String dinerName = resolveDinerName(order.getUserId());
            Map<String, String> data = buildData(order, dinerName);
            String page = resolvePage();
            String state = resolveMiniprogramState();
            boolean ok = wxSubscribeMessageService.send(chef.getOpenid(), templateId, page, state, data);
            if (ok) {
                log.info("chef wx subscribe sent orderNo={} kitchenId={}", order.getOrderNo(), order.getKitchenId());
            }
        } catch (Exception ex) {
            log.warn("chef wx notify failed orderNo={}: {}", order.getOrderNo(), ex.toString());
        }
    }

    /**
     * 供小程序读取：是否可发起订阅授权及模板 ID。
     *
     * @return templateId 可能为空
     */
    public String resolveTemplateId() {
        String fromDb = systemConfigReader.getRaw(CFG_TEMPLATE_ID);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : "";
    }

    /**
     * 系统配置总开关。
     *
     * @return true 开启
     */
    public boolean isEnabled() {
        return systemConfigReader.getBoolean(CFG_ENABLED, false);
    }

    /**
     * 消息跳转页；缺省 {@value #DEFAULT_PAGE}。
     *
     * @return 小程序路径
     */
    public String resolvePage() {
        String fromDb = systemConfigReader.getRaw(CFG_PAGE);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : DEFAULT_PAGE;
    }

    /**
     * 跳转小程序版本态；缺省 formal。
     *
     * @return formal / trial / developer
     */
    public String resolveMiniprogramState() {
        String fromDb = systemConfigReader.getRaw(CFG_MINIPROGRAM_STATE);
        return StringUtils.hasText(fromDb) ? fromDb.trim() : DEFAULT_MINIPROGRAM_STATE;
    }

    private String resolveDinerName(Long userId) {
        if (userId == null) {
            return "食客";
        }
        UserEntity user = userRepository.selectById(userId);
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return "食客";
    }

    /**
     * 默认字段名须与微信公众平台所选模板一致；若模板字段不同请在平台调整或改本映射。
     */
    private Map<String, String> buildData(Order order, String dinerName) {
        String dishes = summarizeDishes(order);
        String remark = StringUtils.hasText(order.getRemark()) ? order.getRemark() : "无";
        String time = order.getCreateTime() == null
                ? TIME_FMT.format(java.time.Instant.now())
                : TIME_FMT.format(order.getCreateTime());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("thing1", dishes);
        data.put("character_string2", order.getOrderNo() == null ? "" : order.getOrderNo());
        data.put("thing3", dinerName);
        data.put("time4", time);
        data.put("thing5", remark);
        return data;
    }

    private String summarizeDishes(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "新预约";
        }
        return order.getItems().stream()
                .map(OrderItem::getDishName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }
}
