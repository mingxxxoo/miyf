package cn.miyf.oss.security;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.oss.cache.OssCacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件访问权限 Redis 工具：响应授权写入、读取校验、文件 ID 解析。
 * <p>
 * 缓存键见 {@link OssCacheKeys#fileAccess}；TTL 约 5 小时并加随机抖动，避免同时失效。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Component
@RequiredArgsConstructor
public class FileAccessPermissionCache {

    private static final Pattern FILE_ID_IN_URL = Pattern.compile("/r/(\\d+)(?:\\D|$)");
    private static final Duration BASE_TTL = Duration.ofHours(5);
    private static final int JITTER_SECONDS = 300;

    private final CacheClient cacheClient;

    /**
     * 为当前登录主体授予文件临时访问权（未登录则忽略）。
     *
     * @param fileId 文件 ID
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void grant(Long fileId) {
        if (fileId == null) {
            return;
        }
        Optional<AuthPrincipal> principal = SecurityUtils.currentPrincipal();
        if (principal.isEmpty()) {
            return;
        }
        put(principal.get(), fileId);
    }

    /**
     * 批量授予。
     *
     * @param fileIds 文件 ID 集合
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void grantAll(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Optional<AuthPrincipal> principal = SecurityUtils.currentPrincipal();
        if (principal.isEmpty()) {
            return;
        }
        AuthPrincipal p = principal.get();
        for (Long fileId : fileIds) {
            if (fileId != null) {
                put(p, fileId);
            }
        }
    }

    /**
     * 当前主体是否持有该文件的缓存访问权。
     *
     * @param fileId 文件 ID
     * @return true 表示缓存命中
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public boolean hasAccess(Long fileId) {
        if (fileId == null || !cacheClient.isEnabled()) {
            return false;
        }
        return SecurityUtils.currentPrincipal()
                .map(p -> cacheClient.objects(Boolean.class).get(cacheKey(p, fileId)).orElse(Boolean.FALSE))
                .orElse(Boolean.FALSE);
    }

    /**
     * 从字段值解析文件 ID（支持 Long、数字字符串、{@code /r/{id}} URL）。
     *
     * @param value 字段值
     * @return 文件 ID；无法解析则 empty
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public Optional<Long> parseFileId(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Long longId) {
            return Optional.of(longId);
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value instanceof String text) {
            return parseFileId(text);
        }
        return Optional.empty();
    }

    /**
     * 从字符串解析文件 ID。
     *
     * @param text URL 或纯数字 ID
     * @return 文件 ID
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public Optional<Long> parseFileId(String text) {
        if (!StringUtils.hasText(text)) {
            return Optional.empty();
        }
        String raw = text.trim();
        Matcher matcher = FILE_ID_IN_URL.matcher(raw);
        if (matcher.find()) {
            return Optional.of(Long.parseLong(matcher.group(1)));
        }
        if (raw.chars().allMatch(Character::isDigit)) {
            try {
                return Optional.of(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 随机 TTL：5 小时基准 + [0, 300) 秒抖动。
     *
     * @return TTL
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public Duration randomTtl() {
        int jitter = ThreadLocalRandom.current().nextInt(JITTER_SECONDS);
        return BASE_TTL.plusSeconds(jitter);
    }

    private void put(AuthPrincipal principal, Long fileId) {
        if (!cacheClient.isEnabled()) {
            return;
        }
        cacheClient.objects(Boolean.class).put(cacheKey(principal, fileId), Boolean.TRUE, randomTtl());
    }

    private static String cacheKey(AuthPrincipal principal, Long fileId) {
        PrincipalType type = principal.getType() == null ? PrincipalType.USER : principal.getType();
        Long id = principal.getId() == null ? 0L : principal.getId();
        return OssCacheKeys.fileAccess(type.name().toLowerCase(), id, fileId);
    }
}
