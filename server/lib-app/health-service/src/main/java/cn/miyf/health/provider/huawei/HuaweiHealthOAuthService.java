package cn.miyf.health.provider.huawei;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.infrastructure.redis.RedisAtomicOps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * <p>
 * 支持管理端与个人端两套 redirect_uri；state 绑定发起主体，防 CSRF。
 * refresh 仅校验 clientId/clientSecret，不依赖 redirect_uri。
 *
 * @author XieMingJie
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Component
@Slf4j
public class HuaweiHealthOAuthService {

    public static final String TOKEN_KEY_PREFIX = "miyf:health:huawei:token:";
    public static final String STATE_KEY_PREFIX = "miyf:health:huawei:oauth:state:";

    /**
     * OAuth 发起渠道：决定 redirect_uri 与登录主体类型校验。
     */
    public enum OAuthChannel {
        /** 管理端 SPA 回调 */
        ADMIN,
        /** 个人端（小程序用户）服务端回调 */
        USER
    }

    private final HealthProperties healthProperties;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * 构造华为 OAuth 服务。
     *
     * @param healthProperties 健康配置
     * @param redisProvider    Redis（存 token / state）
     * @param objectMapper     JSON
     * @param huaweiRestClient 带超时的 HTTP 客户端
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HuaweiHealthOAuthService(HealthProperties healthProperties,
                                    ObjectProvider<StringRedisTemplate> redisProvider,
                                    ObjectMapper objectMapper,
                                    @Qualifier("huaweiRestClient") RestClient huaweiRestClient) {
        this.healthProperties = healthProperties;
        this.redisProvider = redisProvider;
        this.objectMapper = objectMapper;
        this.restClient = huaweiRestClient;
    }

    /**
     * 管理端授权 URL（兼容旧调用）。
     * 现委托 {@link #buildAuthorizeUrl(Long, OAuthChannel)}，渠道固定为 ADMIN。
     *
     * @param subjectId 健康主体
     * @return 授权 URL
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String buildAuthorizeUrl(Long subjectId) {
        return buildAuthorizeUrl(subjectId, OAuthChannel.ADMIN);
    }

    /**
     * 个人端授权 URL。
     *
     * @param subjectId 健康主体
     * @return 授权 URL
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public String buildUserAuthorizeUrl(Long subjectId) {
        return buildAuthorizeUrl(subjectId, OAuthChannel.USER);
    }

    /**
     * 生成授权跳转 URL，并将 state 写入 Redis（15 分钟）。
     *
     * @param subjectId 健康主体
     * @param channel   管理端 / 个人端
     * @return 授权 URL
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public String buildAuthorizeUrl(Long subjectId, OAuthChannel channel) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        if (channel == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth channel 不能为空");
        }
        AuthPrincipal principal = SecurityUtils.requirePrincipal();
        if (channel == OAuthChannel.ADMIN) {
            if (principal.getType() != PrincipalType.ADMIN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员登录");
            }
        } else if (principal.getType() != PrincipalType.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要用户登录");
        }
        Long principalId = principal.getId();
        if (principalId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无法识别当前登录主体");
        }
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        String redirectUri = resolveRedirectUri(cfg, channel);
        requireClientConfigured(cfg, redirectUri);
        String state = UUID.randomUUID().toString().replace("-", "");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("subjectId", subjectId);
        payload.put("principalId", principalId);
        payload.put("principalType", principal.getType().name());
        payload.put("redirectUri", redirectUri);
        try {
            redis().opsForValue().set(
                    STATE_KEY_PREFIX + state,
                    objectMapper.writeValueAsString(payload),
                    Duration.ofMinutes(15));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存 OAuth state 失败");
        }
        String scope = cfg.getScopes().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        return cfg.getAuthorizeUrl()
                + "?response_type=code"
                + "&client_id=" + enc(cfg.getClientId())
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc(scope)
                + "&access_type=offline"
                + "&state=" + enc(state);
    }

    /**
     * 用授权 code 换 token 并缓存；强制消费 state，并校验发起主体与当前登录一致。
     * 支持 USER/ADMIN；换票所用 redirect_uri 取自 state（须与发起授权时一致）。
     * 用于管理端 SPA / 个人端已登录场景的 POST callback。
     *
     * @param subjectId 主体（可选，须与 state 一致）
     * @param code      授权码
     * @param state     OAuth state
     * @return 主体与 token
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public OAuthResult exchangeCode(Long subjectId, String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "授权 code 不能为空");
        }
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 不能为空");
        }
        HuaweiOAuthStatePayload statePayload = consumeState(state);
        AuthPrincipal current = SecurityUtils.requirePrincipal();
        if (current.getId() == null
                || !current.getId().equals(statePayload.principalId())
                || current.getType() != statePayload.principalType()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "OAuth state 与当前登录主体不匹配");
        }
        return exchangeWithPayload(subjectId, code, statePayload);
    }

    /**
     * 服务端浏览器回调换票：仅校验并消费 state（标准 OAuth redirect），无需二次登录。
     *
     * @param code  授权码
     * @param state OAuth state
     * @return 主体与 token
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public OAuthResult exchangeCodeFromRedirect(String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "授权 code 不能为空");
        }
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 不能为空");
        }
        HuaweiOAuthStatePayload statePayload = consumeState(state);
        // 公开回跳入口仅接受个人端发起的授权，拒绝 ADMIN state
        if (statePayload.principalType() != PrincipalType.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该回调仅支持个人端华为授权");
        }
        return exchangeWithPayload(null, code, statePayload);
    }

    private OAuthResult exchangeWithPayload(Long subjectId, String code, HuaweiOAuthStatePayload statePayload) {
        if (subjectId != null && !subjectId.equals(statePayload.subjectId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 与主体不匹配");
        }
        Long sid = statePayload.subjectId();
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        String redirectUri = statePayload.redirectUri();
        requireClientConfigured(cfg, redirectUri);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code.trim());
        form.add("client_id", cfg.getClientId());
        form.add("client_secret", cfg.getClientSecret());
        form.add("redirect_uri", redirectUri);
        HuaweiTokenBundle bundle = requestToken(form);
        bundle.setSource("oauth");
        saveToken(sid, bundle);
        return new OAuthResult(sid, bundle);
    }

    /**
     * OAuth 换票结果。
     *
     * @param subjectId 主体
     * @param token     Token 包
     */
    public record OAuthResult(Long subjectId, HuaweiTokenBundle token) {
    }

    /**
     * 读取缓存 token（不刷新）。
     *
     * @param subjectId 主体
     * @return token，无则 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HuaweiTokenBundle peekToken(Long subjectId) {
        return loadToken(subjectId);
    }

    /**
     * 获取可用 access_token；过期则 refresh 后写回 Redis。
     *
     * @param subjectId 主体
     * @return 有效 token 包
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HuaweiTokenBundle requireAccessToken(Long subjectId) {
        HuaweiTokenBundle bundle = loadToken(subjectId);
        if (bundle == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "该主体尚未完成华为 OAuth 授权");
        }
        Instant now = Instant.now();
        if (bundle.accessTokenValid(now)) {
            return bundle;
        }
        if (!StringUtils.hasText(bundle.getRefreshToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "华为 access_token 已过期，请重新 OAuth 授权");
        }
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        requireClientCredentials(cfg);
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

    /**
     * 是否已缓存 access_token。
     *
     * @param subjectId 主体
     * @return true 已授权
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean hasToken(Long subjectId) {
        HuaweiTokenBundle bundle = loadToken(subjectId);
        return bundle != null && StringUtils.hasText(bundle.getAccessToken());
    }

    /**
     * 清除主体的华为 token 缓存。
     *
     * @param subjectId 主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void clearToken(Long subjectId) {
        redis().delete(tokenKey(subjectId));
    }

    /**
     * 绑定表 credential_ref：Redis key 前缀 + subjectId。
     *
     * @param subjectId 主体
     * @return 凭证引用
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
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
            String stored = HuaweiTokenCrypto.seal(json, tokenEncryptSecret());
            Duration ttl = Duration.ofDays(30);
            if (bundle.getExpiresTime() != null) {
                Duration untilExpire = Duration.between(Instant.now(), bundle.getExpiresTime().plus(Duration.ofDays(14)));
                if (!untilExpire.isNegative() && untilExpire.compareTo(ttl) > 0) {
                    ttl = untilExpire;
                }
            }
            redis().opsForValue().set(tokenKey(subjectId), stored, ttl);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存华为 token 失败");
        }
    }

    private HuaweiTokenBundle loadToken(Long subjectId) {
        String stored = redis().opsForValue().get(tokenKey(subjectId));
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        try {
            String json = HuaweiTokenCrypto.open(stored, tokenEncryptSecret());
            return objectMapper.readValue(json, HuaweiTokenBundle.class);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("parse huawei token failed subjectId={}", subjectId);
            return null;
        }
    }

    private String tokenEncryptSecret() {
        HealthProperties.Huawei cfg = healthProperties.getHuawei();
        if (StringUtils.hasText(cfg.getTokenEncryptSecret())) {
            return cfg.getTokenEncryptSecret().trim();
        }
        // 回落 clientSecret，保证有密钥材料时默认加密
        return cfg.getClientSecret();
    }

    /**
     * 原子消费 OAuth state。
     * 支持 principalType、redirectUri；旧格式仅含 adminId 时视为 ADMIN，并按类型回落配置中的 redirect。
     *
     * @param state state（必填）
     * @return 载荷
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public HuaweiOAuthStatePayload consumeState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 不能为空");
        }
        String key = STATE_KEY_PREFIX + state.trim();
        String raw = RedisAtomicOps.getAndDelete(redis(), key);
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 无效或已过期");
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            // 兼容旧格式：纯数字 subjectId 一律拒绝，强制重新授权
            if (node.isNumber() || (!node.isObject() && StringUtils.hasText(raw) && raw.chars().allMatch(Character::isDigit))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 已失效，请重新发起授权");
            }
            Long subjectId = readLong(node, "subjectId");
            Long principalId = node.hasNonNull("principalId")
                    ? readLong(node, "principalId")
                    : readLong(node, "adminId");
            PrincipalType principalType = PrincipalType.ADMIN;
            if (node.hasNonNull("principalType") && StringUtils.hasText(node.path("principalType").asText())) {
                principalType = PrincipalType.valueOf(node.path("principalType").asText().trim());
            }
            String redirectUri = node.path("redirectUri").asText(null);
            if (!StringUtils.hasText(redirectUri)) {
                // 旧 state 无 redirectUri：按主体类型回落到配置
                HealthProperties.Huawei cfg = healthProperties.getHuawei();
                redirectUri = principalType == PrincipalType.USER
                        ? resolveRedirectUri(cfg, OAuthChannel.USER)
                        : resolveRedirectUri(cfg, OAuthChannel.ADMIN);
            }
            if (subjectId == null || principalId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 损坏");
            }
            return new HuaweiOAuthStatePayload(subjectId, principalId, principalType, redirectUri);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 损坏");
        }
    }

    /**
     * @deprecated 请使用 {@link #consumeState(String)}
     */
    @Deprecated
    public Long consumeStateSubject(String state) {
        return consumeState(state).subjectId();
    }

    /**
     * 解析 OAuth state 对应的主体（只读，不删除）；无效或过期抛业务异常。
     *
     * @param state state
     * @return 主体 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     * @deprecated 换票请使用 {@link #consumeState(String)}
     */
    @Deprecated
    public Long resolveStateSubject(String state) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 不能为空");
        }
        String v = redis().opsForValue().get(STATE_KEY_PREFIX + state.trim());
        if (!StringUtils.hasText(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 无效或已过期");
        }
        try {
            JsonNode node = objectMapper.readTree(v);
            if (node.isObject()) {
                return node.path("subjectId").asLong();
            }
            return Long.parseLong(v);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth state 损坏");
        }
    }

    private static Long readLong(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return v.asLong();
        }
        String text = v.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.parseLong(text.trim());
    }

    private static String resolveRedirectUri(HealthProperties.Huawei cfg, OAuthChannel channel) {
        if (channel == OAuthChannel.USER) {
            return StringUtils.hasText(cfg.getUserRedirectUri())
                    ? cfg.getUserRedirectUri().trim()
                    : "";
        }
        return cfg.getRedirectUri() == null ? "" : cfg.getRedirectUri().trim();
    }

    private void requireClientConfigured(HealthProperties.Huawei cfg, String redirectUri) {
        requireClientCredentials(cfg);
        if (!StringUtils.hasText(redirectUri)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置华为 redirectUri");
        }
    }

    /**
     * 校验华为 client 凭证（token refresh 等不需要 redirect_uri 的场景）。
     *
     * @param cfg 华为配置
     */
    private void requireClientCredentials(HealthProperties.Huawei cfg) {
        if (!StringUtils.hasText(cfg.getClientId()) || !StringUtils.hasText(cfg.getClientSecret())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "未配置华为 clientId/clientSecret（app.health.huawei.*）");
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
