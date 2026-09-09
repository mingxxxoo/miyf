package cn.miyf.auth.infrastructure.wx;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 真实微信 code2session 实现。
 * <p>
 * 微信接口常返回 {@code Content-Type: text/plain}，不可直接按 application/json 反序列化。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Component
@ConditionalOnProperty(prefix = "wx.auth", name = "mock-enabled", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class RealWxAuthClient implements WxAuthClient {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WxAuthProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     * @history 1.01 2026-09-09 XieMingJie 兼容微信 text/plain 响应体.
     */
    @Override
    public WxSession code2Session(String code) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()
                || properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "未配置微信 AppId/AppSecret");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "微信登录 code 为空");
        }
        String uri = UriComponentsBuilder.fromUriString(CODE2SESSION_URL)
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        try {
            // 微信常返回 text/plain，先取字符串再解析 JSON
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (!StringUtils.hasText(body)) {
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "微信登录无响应");
            }
            JsonNode node = objectMapper.readTree(body);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                int errcode = node.path("errcode").asInt();
                String errmsg = node.path("errmsg").asText("");
                // 不记录 secret；记录微信业务错误便于排查 appid/code 问题
                log.warn("WeChat code2session failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "微信登录失败(" + errcode + ")");
            }
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isBlank()) {
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "微信未返回 openid");
            }
            String unionid = node.path("unionid").asText(null);
            return new WxSession(openid, unionid);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("WeChat code2session error", ex);
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED);
        }
    }
}
