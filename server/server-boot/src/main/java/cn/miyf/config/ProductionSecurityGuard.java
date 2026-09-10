package cn.miyf.config;

import cn.miyf.auth.infrastructure.wx.WxAuthProperties;
import cn.miyf.auth.security.JwtProperties;
import cn.miyf.health.config.HealthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 生产/容器环境启动安全门禁：拒绝危险默认密钥与 Mock/公开桶配置。
 * 校验范围含 JWT、数据源密码、MinIO 账号，以及文件签名密钥 access-sign-secret。
 * 仅在 {@code prod}、{@code docker} profile 生效，本地 {@code dev} 不受影响。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ProductionSecurityGuard implements ApplicationRunner {

    private static final Set<String> GUARDED_PROFILES = Set.of("prod", "docker");
    private static final Set<String> FORBIDDEN_EXACT = Set.of(
            "change-me",
            "change-me-to-a-long-random-secret-at-least-32-chars",
            "change-me-file-access-sign-secret-32chars",
            "minioadmin"
    );

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final FileStorageProperties fileStorageProperties;
    private final WxAuthProperties wxAuthProperties;
    private final HealthProperties healthProperties;

    /**
     * 校验密钥、Mock 与公开读开关；不通过则拒绝启动。
     * 包含 FILE_STORAGE_ACCESS_SIGN_SECRET（文件签名 URL HMAC 密钥）强度检查。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!isGuardedProfileActive()) {
            return;
        }
        List<String> errors = new ArrayList<>();
        rejectInsecure("JWT_SECRET / app.jwt.secret", jwtProperties.getSecret(), 32, errors);
        rejectInsecure("PRIMARY_DB_PASSWORD / app.datasource.primary.password",
                environment.getProperty("app.datasource.primary.password"), 8, errors);
        rejectInsecure("FILE_STORAGE_ACCESS_KEY", fileStorageProperties.getAccessKey(), 8, errors);
        rejectInsecure("FILE_STORAGE_SECRET_KEY", fileStorageProperties.getSecretKey(), 8, errors);
        rejectInsecure("FILE_STORAGE_ACCESS_SIGN_SECRET / app.file-storage.access-sign-secret",
                fileStorageProperties.getAccessSignSecret(), 32, errors);

        if (wxAuthProperties.isMockEnabled()) {
            errors.add("wx.auth.mock-enabled 在生产/容器环境必须为 false（设置 WX_AUTH_MOCK_ENABLED=false）");
        }
        if (healthProperties.getHuawei().isMockEnabled()) {
            errors.add("app.health.huawei.mock-enabled 在生产/容器环境必须为 false（设置 APP_HEALTH_HUAWEI_MOCK=false）");
        }
        if (fileStorageProperties.isPublicRead()) {
            errors.add("app.file-storage.public-read 在生产/容器环境必须为 false（设置 FILE_STORAGE_PUBLIC_READ=false）");
        }

        if (!errors.isEmpty()) {
            String message = "生产安全校验失败，拒绝启动:\n - " + String.join("\n - ", errors);
            log.error(message);
            throw new IllegalStateException(message);
        }
        log.info("Production security guard passed for profiles {}",
                String.join(",", environment.getActiveProfiles()));
    }

    private boolean isGuardedProfileActive() {
        for (String profile : environment.getActiveProfiles()) {
            if (GUARDED_PROFILES.contains(profile.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void rejectInsecure(String name, String value, int minLen, List<String> errors) {
        if (!StringUtils.hasText(value)) {
            errors.add(name + " 未配置");
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() < minLen) {
            errors.add(name + " 长度不足（至少 " + minLen + "）");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_EXACT.contains(lower) || lower.contains("change-me")) {
            errors.add(name + " 仍为不安全默认值，请通过环境变量替换");
        }
    }
}
