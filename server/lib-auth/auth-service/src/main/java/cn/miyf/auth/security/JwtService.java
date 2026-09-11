package cn.miyf.auth.security;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析。
 * <p>
 * claim：uid / typ / perms / org / ds / tv；管理员与用户共用同一密钥，靠 typ 区分。
 * tv 为 tokenVersion，禁用账号时递增以使旧令牌失效；版本比对由 {@link TokenVersionService} 负责（含短缓存与读失败 fail-open）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 * @history 1.00 2026-09-04 17:06 XieMingJie Created.
 */
@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_PERMS = "perms";
    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_DS = "ds";
    private static final String CLAIM_TV = "tv";

    private final JwtProperties properties;
    private final TokenVersionService tokenVersionService;
    private final SecretKey secretKey;

    /**
     * @param properties          JWT 配置
     * @param tokenVersionService 版本服务
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public JwtService(JwtProperties properties, TokenVersionService tokenVersionService) {
        this.properties = properties;
        this.tokenVersionService = tokenVersionService;
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret 未配置");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 拒绝短密钥填充，避免弱密钥被放大为看似合法的 HMAC key
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 长度不足 32 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 JWT（写入当前 tokenVersion）。
     *
     * @param principal 主体
     * @return token
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public String createToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpireSeconds());
        long tv = tokenVersionService.current(principal.getType(), principal.getId());
        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_UID, principal.getId().toString())
                .claim(CLAIM_TYP, principal.getType().name())
                .claim(CLAIM_PERMS, String.join(",", principal.getPermissions()))
                .claim(CLAIM_DS, principal.getDataScope().name())
                .claim(CLAIM_TV, tv)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey);
        if (principal.getOrgUnitId() != null) {
            builder.claim(CLAIM_ORG, principal.getOrgUnitId().toString());
        }
        return builder.compact();
    }

    /**
     * 解析并校验 tokenVersion；版本不匹配视为无效。
     *
     * @param token JWT
     * @return 主体
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long uid = Long.parseLong(claims.get(CLAIM_UID, String.class));
        PrincipalType type = PrincipalType.valueOf(claims.get(CLAIM_TYP, String.class));
        long claimTv = 0L;
        Object tvObj = claims.get(CLAIM_TV);
        if (tvObj instanceof Number number) {
            claimTv = number.longValue();
        } else if (tvObj != null && StringUtils.hasText(tvObj.toString())) {
            claimTv = Long.parseLong(tvObj.toString().trim());
        }
        if (!tokenVersionService.matches(type, uid, claimTv)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        String perms = claims.get(CLAIM_PERMS, String.class);
        List<String> permissions = (perms == null || perms.isBlank())
                ? List.of()
                : Arrays.asList(perms.split(","));
        Long orgUnitId = null;
        String org = claims.get(CLAIM_ORG, String.class);
        if (StringUtils.hasText(org)) {
            try {
                orgUnitId = Long.parseLong(org.trim());
            } catch (NumberFormatException ignored) {
                orgUnitId = null;
            }
        }
        DataScope dataScope = DataScope.parse(claims.get(CLAIM_DS, String.class));
        return new AuthPrincipal(uid, claims.getSubject(), type, permissions, true, orgUnitId, dataScope);
    }

    public long getExpireSeconds() {
        return properties.getExpireSeconds();
    }
}
