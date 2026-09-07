package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.config.HealthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 华为 OAuth2：授权链接、code 换 token、refresh、Redis 缓存。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class HuaweiHealthOAuthService {

    private static final Logger log = LoggerFactory.getLogger(HuaweiHealthOAuthService.class);
    public static final String TOKEN_KEY_PREFIX = "miyf:health:huawei:token:";
    public static final String STATE_KEY_PREFIX = "miyf:health:huawei:oauth:state:";

    private final HealthProperties healthProperties;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HuaweiHealthOAuthService(HealthProperties healthProperties,
                                    ObjectProvider<StringRedisTemplate> redisProvider,
                                    ObjectMapper objectMapper) {
        this.healthProperties = healthProperties;
        this.redisProvider = redisProvider;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public String buildAuthorizeUrl(Long subjectId) {
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        requireClientConfigured(cfg);
        String state = UUID.randomUUID().toString().replace("-", "");
        redis().opsForValue().set(STATE_KEY_PREFIX + state, String.valueOf(subjectId), Duration.ofMinutes(15));
        String scope = cfg.getScopes().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        return cfg.getAuthorizeUrl()
                + "?response_type=code"
                + "&client_id=" + enc(cfg.getClientId())
                + "&redirect_uri=" + enc(cfg.getRedirectUri())
                + "&scope=" + enc(scope)
                + "&access_type=offline"
                + "&state=" + enc(state);
    }

    public OAuthResult exchangeCode(Long subjectId, String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "授权 code 不能为空");
        }
        Long stateSubject = resolveStateSubject(state);
        if (stateSubject != null && subjectId != null && !stateSubject.equals(subjectId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 与主体不匹配");
        }
        Long sid = subjectId != null ? subjectId : stateSubject;
        if (sid == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        requireClientConfigured(cfg);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code.trim());
        form.add("client_id", cfg.getClientId());
        form.add("client_secret", cfg.getClientSecret());
        form.add("redirect_uri", cfg.getRedirectUri());
        HuaweiTokenBundle bundle = requestToken(form);
        bundle.setSource("oauth");
        saveToken(sid, bundle);
        if (StringUtils.hasText(state)) {
            redis().delete(STATE_KEY_PREFIX + state.trim());
        }
        return new OAuthResult(sid, bundle);
    }

    public record OAuthResult(Long subjectId, HuaweiTokenBundle token) {
    }

    public HuaweiTokenBundle peekToken(Long subjectId) {
        return loadToken(subjectId);
    }

    public HuaweiTokenBundle requireAccessToken(Long subjectId) {
        HuaweiTokenBundle bundle = loadToken(subjectId);
        if (bundle == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "该主体尚未完成华为 OAuth 授权");
        }
        Instant now = Instant.now();
        if (bundle.accessTokenValid(now)) {
            return bundle;
        }
        if (!StringUtils.hasText(bundle.getRefreshToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "华为 access_token 已过期，请重新 OAuth 授权");
        }
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        requireClientConfigured(cfg);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", bundle.getRefreshToken());
        form.add("client_id", cfg.getClientId());
        form.add("client_secret", cfg.getClientSecret());
        HuaweiTokenBundle refreshed = requestToken(form);
        if (!StringUtils.hasText(refreshed.getRefreshToken())) {
            refreshed.setRefreshToken(bundle.getRefreshToken());
        }
        if (!StringUtils.hasText(refreshed.getOpenId())) {
            refreshed.setOpenId(bundle.getOpenId());
        }
        refreshed.setSource("oauth");
        saveToken(subjectId, refreshed);
        return refreshed;
    }

    public boolean hasToken(Long subjectId) {
        HuaweiTokenBundle bundle = loadToken(subjectId);
        return bundle != null && StringUtils.hasText(bundle.getAccessToken());
    }

    public void clearToken(Long subjectId) {
        redis().delete(tokenKey(subjectId));
    }

    public String credentialRef(Long subjectId) {
        return TOKEN_KEY_PREFIX + subjectId;
    }

    private HuaweiTokenBundle requestToken(MultiValueMap<String, String> form) {
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        try {
            String body = restClient.post()
                    .uri(cfg.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (root.has("error")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "华为 token 失败: " + root.path("error_description").asText(root.path("error").asText()));
            }
            String access = root.path("access_token").asText(null);
            if (!StringUtils.hasText(access)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "华为未返回 access_token");
            }
            long expiresIn = root.path("expires_in").asLong(3600L);
            return new HuaweiTokenBundle()
                    .setAccessToken(access)
                    .setRefreshToken(root.path("refresh_token").asText(null))
                    .setExpiresTime(Instant.now().plusSeconds(Math.max(60, expiresIn)))
                    .setScope(root.path("scope").asText(null))
                    .setOpenId(root.path("open_id").asText(null))
                    .setSource("oauth");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("huawei token request failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "调用华为 OAuth 失败: " + ex.getMessage());
        }
    }

    private void saveToken(Long subjectId, HuaweiTokenBundle bundle) {
        try {
            String json = objectMapper.writeValueAsString(bundle);
            Duration ttl = Duration.ofDays(30);
            if (bundle.getExpiresTime() != null) {
                Duration untilExpire = Duration.between(Instant.now(), bundle.getExpiresTime().plus(Duration.ofDays(14)));
                if (!untilExpire.isNegative() && untilExpire.compareTo(ttl) > 0) {
                    ttl = untilExpire;
                }
            }
            redis().opsForValue().set(tokenKey(subjectId), json, ttl);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存华为 token 失败");
        }
    }

    private HuaweiTokenBundle loadToken(Long subjectId) {
        String json = redis().opsForValue().get(tokenKey(subjectId));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, HuaweiTokenBundle.class);
        } catch (Exception ex) {
            log.warn("parse huawei token failed subjectId={}", subjectId);
            return null;
        }
    }

    public Long resolveStateSubject(String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        String v = redis().opsForValue().get(STATE_KEY_PREFIX + state.trim());
        if (!StringUtils.hasText(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 无效或已过期");
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 损坏");
        }
    }

    private void requireClientConfigured(HealthProperties.Huawei cfg) {
        if (!StringUtils.hasText(cfg.getClientId()) || !StringUtils.hasText(cfg.getClientSecret())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "未配置华为 clientId/clientSecret（app.health.huawei.*）");
        }
        if (!StringUtils.hasText(cfg.getRedirectUri())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置华为 redirectUri");
        }
    }

    private StringRedisTemplate redis() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Redis 不可用，无法存储华为 OAuth Token");
        }
        return redis;
    }

    private static String tokenKey(Long subjectId) {
        return TOKEN_KEY_PREFIX + subjectId;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
