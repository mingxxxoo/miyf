package cn.miyf.auth.infrastructure.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 真实华为账号换票：授权码换 token，再解析 id_token；缺 unionId 时再查用户信息。
 * 不记录授权码、token 与 clientSecret。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@Component
@ConditionalOnProperty(prefix = "app.auth.huawei", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class RealHuaweiAccountClient implements HuaweiAccountClient {

    private final HuaweiAccountProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * @param properties   华为账号配置
     * @param restClient   复用微信模块的超时客户端
     * @param objectMapper JSON
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    public RealHuaweiAccountClient(HuaweiAccountProperties properties,
                                   @Qualifier("wxRestClient") RestClient restClient,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    @Override
    public HuaweiIdentity exchange(String authorizationCode) {
        if (!StringUtils.hasText(properties.getClientId()) || !StringUtils.hasText(properties.getClientSecret())) {
            throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "未配置华为账号 ClientId/ClientSecret");
        }
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "华为授权码为空");
        }
        try {
            JsonNode token = postForm(properties.getTokenUrl(),
                    "grant_type=authorization_code&code=" + enc(authorizationCode.trim())
                            + "&client_id=" + enc(properties.getClientId())
                            + "&client_secret=" + enc(properties.getClientSecret()));
            if (token.hasNonNull("error")) {
                log.warn("Huawei token exchange failed: error={}", token.path("error").asText(""));
                throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "华为授权码无效");
            }
            String idToken = token.path("id_token").asText("");
            String accessToken = token.path("access_token").asText("");
            if (!StringUtils.hasText(idToken) && !StringUtils.hasText(accessToken)) {
                throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "华为未返回登录票据");
            }
            JsonNode info = StringUtils.hasText(idToken)
                    ? postForm(properties.getTokenInfoUrl(), "id_token=" + enc(idToken))
                    : objectMapper.createObjectNode();
            String openId = firstText(info, "sub", "openID", "openid");
            String unionId = firstText(info, "union_id", "unionID", "unionid");
            String displayName = firstText(info, "display_name", "displayName");
            String phone = firstText(info, "phone", "mobileNumber", "purePhoneNumber");
            if (!StringUtils.hasText(unionId) && StringUtils.hasText(accessToken)) {
                JsonNode profile = postForm(properties.getUserInfoUrl() + "?nsp_svc=GOpen.User.getInfo",
                        "access_token=" + enc(accessToken) + "&getNickName=1");
                openId = firstNonBlank(openId, firstText(profile, "openID", "openid"));
                unionId = firstText(profile, "unionID", "unionId", "union_id");
                displayName = firstNonBlank(displayName, firstText(profile, "displayName", "nickName"));
                phone = firstNonBlank(phone, firstText(profile, "mobileNumber", "phone"));
            }
            if (!StringUtils.hasText(openId) || !StringUtils.hasText(unionId)) {
                throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "华为未返回用户标识");
            }
            return new HuaweiIdentity(openId, unionId, displayName, phone);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Huawei account exchange error");
            throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED);
        }
    }

    private JsonNode postForm(String url, String form) throws Exception {
        String body = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.HUAWEI_AUTH_FAILED, "华为登录无响应");
        }
        return objectMapper.readTree(body);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String firstText(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            String text = node.path(name).asText("");
            if (StringUtils.hasText(text) && !"null".equals(text)) {
                return text.trim();
            }
        }
        return null;
    }

    private static String firstNonBlank(String current, String fallback) {
        return StringUtils.hasText(current) ? current : fallback;
    }
}
