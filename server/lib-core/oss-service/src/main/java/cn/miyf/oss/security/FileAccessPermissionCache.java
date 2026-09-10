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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件访问权限缓存：业务响应发放短期可读票据、按主体授权、文件 ID 解析。
 * <p>
 * 票据按 {@link OssCacheKeys#fileAccessTicket} 存（与登录主体无关），使 {@code GET /r/{id}}
 * 无需 query/path 凭证即可被 img / 小程序 Image 直开。Redis 关闭时回落到进程内 Map。
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

    /** Redis 关闭时的进程内票据：fileId → 过期 epoch millis。 */
    private final Map<Long, Long> localTickets = new ConcurrentHashMap<>();

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
        putPrincipal(principal.get(), fileId);
    }

    /**
     * 按登录主体批量授予。
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
                putPrincipal(p, fileId);
            }
        }
    }

    /**
     * 发放与主体无关的短期可读票据（{@code @FileAccess} 响应时调用）。
     *
     * @param fileId 文件 ID
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public void grantTicket(Long fileId) {
        if (fileId == null) {
            return;
        }
        Duration ttl = randomTtl();
        if (cacheClient.isEnabled()) {
            cacheClient.objects(Boolean.class).put(OssCacheKeys.fileAccessTicket(fileId), Boolean.TRUE, ttl);
            return;
        }
        localTickets.put(fileId, System.currentTimeMillis() + ttl.toMillis());
    }

    /**
     * 批量发放短期可读票据。
     *
     * @param fileIds 文件 ID 集合
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public void grantTickets(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            grantTicket(fileId);
        }
    }

    /**
     * 是否存在未过期的可读票据（不依赖登录）。
     *
     * @param fileId 文件 ID
     * @return true 表示票据有效
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public boolean hasTicket(Long fileId) {
        if (fileId == null) {
            return false;
        }
        if (cacheClient.isEnabled()) {
            return cacheClient.objects(Boolean.class)
                    .get(OssCacheKeys.fileAccessTicket(fileId))
                    .orElse(Boolean.FALSE);
        }
        Long expireAt = localTickets.get(fileId);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            localTickets.remove(fileId, expireAt);
            return false;
        }
        return true;
    }

    /**
     * 当前请求是否可凭缓存访问该文件：优先检查可读票据，否则查当前主体的临时授权。
     *
     * @param fileId 文件 ID
     * @return true 表示缓存命中
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public boolean hasAccess(Long fileId) {
        if (fileId == null) {
            return false;
        }
        if (hasTicket(fileId)) {
            return true;
        }
        if (!cacheClient.isEnabled()) {
            return false;
        }
        return SecurityUtils.currentPrincipal()
                .map(p -> cacheClient.objects(Boolean.class).get(principalCacheKey(p, fileId)).orElse(Boolean.FALSE))
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

    private void putPrincipal(AuthPrincipal principal, Long fileId) {
        if (!cacheClient.isEnabled()) {
            return;
        }
        cacheClient.objects(Boolean.class).put(principalCacheKey(principal, fileId), Boolean.TRUE, randomTtl());
    }

    private static String principalCacheKey(AuthPrincipal principal, Long fileId) {
        PrincipalType type = principal.getType() == null ? PrincipalType.USER : principal.getType();
        Long id = principal.getId() == null ? 0L : principal.getId();
        return OssCacheKeys.fileAccess(type.name().toLowerCase(), id, fileId);
    }
}
