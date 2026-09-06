package cn.miyf.infrastructure.wx;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 真实微信 code2session 实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Component
@ConditionalOnProperty(prefix = "wx.auth", name = "mock-enabled", havingValue = "false")
public class RealWxAuthClient implements WxAuthClient {

    private static final Logger log = LoggerFactory.getLogger(RealWxAuthClient.class);
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WxAuthProperties properties;
    private final RestClient restClient;

    /**
     * 构造真实微信客户端。
     *
     * @param properties 配置
     * @param restClient HTTP 客户端
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public RealWxAuthClient(WxAuthProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Override
    public WxSession code2Session(String code) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()
                || properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "未配置微信 AppId/AppSecret");
        }
        String uri = UriComponentsBuilder.fromUriString(CODE2SESSION_URL)
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        try {
            JsonNode node = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (node == null || node.has("errcode") && node.get("errcode").asInt() != 0) {
                // 不记录 secret，仅记录业务错误码
                log.warn("WeChat code2session failed: {}", node);
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED);
            }
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isBlank()) {
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED);
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
