package cn.miyf.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 与 BCrypt 基础单测。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
class JwtServiceTest {

    /**
     * 签发后应能解析回同一主体。
     *
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Test
    void shouldCreateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-32bytes-min!!");
        properties.setExpireSeconds(3600);
        JwtService jwtService = new JwtService(properties);

        AuthPrincipal principal = new AuthPrincipal(
                10001L,
                "admin",
                PrincipalType.ADMIN,
                List.of("kitchen:dish:list", "kitchen:order:list"),
                true
        );
        String token = jwtService.createToken(principal);
        AuthPrincipal parsed = jwtService.parseToken(token);

        assertEquals(principal.getId(), parsed.getId());
        assertEquals(PrincipalType.ADMIN, parsed.getType());
        assertTrue(parsed.hasPermission("kitchen:dish:list"));
    }

    /**
     * 种子密码 change-me 应可被 BCrypt 校验（用于核对迁移脚本哈希）。
     *
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Test
    void seedPasswordShouldMatchBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("change-me");
        assertTrue(encoder.matches("change-me", hash));
        // 打印便于更新 V2__seed.sql（测试日志可见）
        System.out.println("BCRYPT_CHANGE_ME=" + hash);
    }
}
