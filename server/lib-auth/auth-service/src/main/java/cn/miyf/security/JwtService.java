package cn.miyf.security;

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
 * claim：uid / typ / perms / org / ds；管理员与用户共用同一密钥，靠 typ 区分。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_PERMS = "perms";
    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_DS = "ds";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpireSeconds());
        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_UID, principal.getId().toString())
                .claim(CLAIM_TYP, principal.getType().name())
                .claim(CLAIM_PERMS, String.join(",", principal.getPermissions()))
                .claim(CLAIM_DS, principal.getDataScope().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey);
        if (principal.getOrgUnitId() != null) {
            builder.claim(CLAIM_ORG, principal.getOrgUnitId().toString());
        }
        return builder.compact();
    }

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
