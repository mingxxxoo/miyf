package cn.miyf.auth.infrastructure.wx;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 微信小程序 access_token 获取与短缓存。
 * Redis 可用时跨实例共享；否则进程内缓存。失败不抛业务异常给调用方以外的路径——由发送方决定。
 *
 * @author XieMingJie
 * @since 2026-09-16
 */
@Component
@Slf4j
public class WxAccessTokenService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String REDIS_KEY = "miyf:wx:mini:access_token";
    /** 提前 5 分钟刷新，避免临界过期 */
    private static final long REFRESH_SKEW_SECONDS = 300L;

    private final WxAuthProperties authProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final AtomicReference<CachedToken> local = new AtomicReference<>();

    public WxAccessTokenService(WxAuthProperties authProperties,
                                @Qualifier("wxRestClient") RestClient restClient,
                                ObjectMapper objectMapper,
                                ObjectProvider<StringRedisTemplate> redisProvider) {
        this.authProperties = authProperties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.redisProvider = redisProvider;
    }

    /**
     * 获取有效 access_token；AppId/Secret 未配置或微信失败时抛业务异常。
     *
     * @return access_token
     */
    public String getAccessToken() {
        String cached = readCache();
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        return refreshAndCache();
    }

    private String readCache() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                String v = redis.opsForValue().get(REDIS_KEY);
                if (StringUtils.hasText(v)) {
                    return v;
                }
            } catch (Exception ex) {
                log.debug("wx access_token redis read failed: {}", ex.toString());
            }
        }
        CachedToken t = local.get();
        if (t != null && t.expireAtEpochMs() > System.currentTimeMillis()) {
            return t.token();
        }
        return null;
    }

    private String refreshAndCache() {
        if (!StringUtils.hasText(authProperties.getAppId()) || !StringUtils.hasText(authProperties.getAppSecret())) {
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "未配置微信 AppId/AppSecret，无法获取 access_token");
        }
        String uri = UriComponentsBuilder.fromUriString(TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", authProperties.getAppId())
                .queryParam("secret", authProperties.getAppSecret())
                .toUriString();
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (!StringUtils.hasText(body)) {
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "获取微信 access_token 无响应");
            }
            JsonNode node = objectMapper.readTree(body);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                int errcode = node.path("errcode").asInt();
                String errmsg = node.path("errmsg").asText("");
                log.warn("WeChat access_token failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "获取微信 access_token 失败(" + errcode + ")");
            }
            String token = node.path("access_token").asText(null);
            long expiresIn = node.path("expires_in").asLong(7200L);
            if (!StringUtils.hasText(token)) {
                throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "微信未返回 access_token");
            }
            long ttl = Math.max(60L, expiresIn - REFRESH_SKEW_SECONDS);
            writeCache(token, ttl);
            return token;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("WeChat access_token error", ex);
            throw new BusinessException(ErrorCode.WX_AUTH_FAILED, "获取微信 access_token 异常");
        }
    }

    private void writeCache(String token, long ttlSeconds) {
        local.set(new CachedToken(token, System.currentTimeMillis() + ttlSeconds * 1000L));
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY, token, Duration.ofSeconds(ttlSeconds));
            } catch (Exception ex) {
                log.debug("wx access_token redis write failed: {}", ex.toString());
            }
        }
    }

    private record CachedToken(String token, long expireAtEpochMs) {
    }
}
