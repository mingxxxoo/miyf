package cn.miyf.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析。
 * <p>
 * claim：uid / typ / perms；管理员与用户共用同一密钥，靠 typ 区分。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_PERMS = "perms";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    /**
     * 构造 JWT 服务。
     *
     * @param properties JWT 配置
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // 密钥长度不足时按 UTF-8 字节补齐到 32，避免 jjwt 校验失败
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 Token。
     *
     * @param principal 登录主体
     * @return JWT 字符串
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public String createToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpireSeconds());
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_UID, principal.getId().toString())
                .claim(CLAIM_TYP, principal.getType().name())
                .claim(CLAIM_PERMS, String.join(",", principal.getPermissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token 为登录主体。
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
        String perms = claims.get(CLAIM_PERMS, String.class);
        List<String> permissions = (perms == null || perms.isBlank())
                ? List.of()
                : Arrays.asList(perms.split(","));
        return new AuthPrincipal(uid, claims.getSubject(), type, permissions, true);
    }

    /**
     * 获取配置过期秒数。
     *
     * @return 过期秒数
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public long getExpireSeconds() {
        return properties.getExpireSeconds();
    }
}
