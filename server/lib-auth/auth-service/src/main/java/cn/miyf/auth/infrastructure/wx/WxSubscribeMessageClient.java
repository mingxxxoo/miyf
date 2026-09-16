package cn.miyf.auth.infrastructure.wx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序订阅消息发送（subscribeMessage.send）。
 * 接收方须曾在小程序内对该模板授权；发送失败仅记日志，由业务决定是否吞掉。
 * 业务参数（开关、模板、跳转页、版本态）由调用方从系统配置读取后传入，本客户端不读 YAML。
 *
 * @author XieMingJie
 * @since 2026-09-16
 */
@Component
@Slf4j
public class WxSubscribeMessageClient {

    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    /** 未指定版本态时默认正式版 */
    private static final String DEFAULT_MINIPROGRAM_STATE = "formal";

    private final WxAccessTokenService accessTokenService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WxSubscribeMessageClient(WxAccessTokenService accessTokenService,
                                    @Qualifier("wxRestClient") RestClient restClient,
                                    ObjectMapper objectMapper) {
        this.accessTokenService = accessTokenService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送订阅消息。
     *
     * @param openid            接收用户 openid
     * @param templateId        模板 ID
     * @param page              跳转页，可空
     * @param miniprogramState  小程序版本态 formal/trial/developer，空则 formal
     * @param data              模板字段，如 thing1 → 文案
     * @return true 微信返回成功；false 跳过或失败
     */
    public boolean send(String openid, String templateId, String page, String miniprogramState,
                        Map<String, String> data) {
        if (!StringUtils.hasText(openid) || !StringUtils.hasText(templateId)) {
            log.debug("skip subscribe send: openid/template blank");
            return false;
        }
        try {
            String token = accessTokenService.getAccessToken();
            String uri = UriComponentsBuilder.fromUriString(SEND_URL)
                    .queryParam("access_token", token)
                    .toUriString();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", openid);
            body.put("template_id", templateId);
            if (StringUtils.hasText(page)) {
                body.put("page", page);
            }
            String state = StringUtils.hasText(miniprogramState)
                    ? miniprogramState.trim()
                    : DEFAULT_MINIPROGRAM_STATE;
            body.put("miniprogram_state", state);
            body.put("lang", "zh_CN");
            body.put("data", toWxData(data));

            String resp = restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(resp)) {
                log.warn("WeChat subscribe send empty response openid={}", mask(openid));
                return false;
            }
            JsonNode node = objectMapper.readTree(resp);
            int errcode = node.path("errcode").asInt(0);
            if (errcode != 0) {
                // 43101 用户拒收 / 未授权；不升 ERROR，避免刷屏
                log.warn("WeChat subscribe send failed: errcode={}, errmsg={}, openid={}",
                        errcode, node.path("errmsg").asText(""), mask(openid));
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("WeChat subscribe send error openid={}: {}", mask(openid), ex.toString());
            return false;
        }
    }

    private static Map<String, Object> toWxData(Map<String, String> data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data == null) {
            return out;
        }
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (!StringUtils.hasText(e.getKey())) {
                continue;
            }
            Map<String, String> cell = new LinkedHashMap<>();
            cell.put("value", truncate(e.getValue()));
            out.put(e.getKey(), cell);
        }
        return out;
    }

    /** 订阅消息 thing 类字段上限约 20 字，统一截断避免 47003 */
    private static String truncate(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        return v.length() <= 20 ? v : v.substring(0, 20);
    }

    private static String mask(String openid) {
        if (!StringUtils.hasText(openid) || openid.length() < 8) {
            return "***";
        }
        return openid.substring(0, 4) + "***" + openid.substring(openid.length() - 4);
    }
}
