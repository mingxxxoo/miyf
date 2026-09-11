package cn.miyf.auth.security;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT tokenVersion：禁用/踢下线时递增，Filter 比对 claim 使旧令牌立即失效。
 * <p>
 * 语义：
 * <ul>
 *   <li>签发（{@link #current}）：Redis 不可用时按 0，不阻断登录</li>
 *   <li>校验（{@link #matches}）：优先本地缓存；Redis 读失败时若本地曾有版本则 fail-closed，
 *   否则 fail-open，避免抖动误杀从未吊销过的会话</li>
 *   <li>吊销（{@link #bump}）：Redis 不可用时抛错；缓存单调递增，防止并发读回写旧版本</li>
 * </ul>
 * 进程内短缓存降低热路径 Redis 压力；bump 写入更长 TTL，降低同机吊销窗口内旧令牌复活。
 *
 * @author XieMingJie
 * @since 2026-09-11
 * @history 1.00 2026-09-11 XieMingJie Created.
 */
@Service
@Slf4j
public class TokenVersionService {

    public static final String KEY_PREFIX = "miyf:auth:tv:";

    /** 普通读缓存 TTL（毫秒）。 */
    private static final long CACHE_TTL_MS = 3_000L;

    /** bump 后缓存 TTL：同机在吊销后更久按新版本 fail-closed。 */
    private static final long BUMP_CACHE_TTL_MS = 60_000L;

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    /**
     * @param redisProvider Redis（可选）
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public TokenVersionService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    /**
     * 当前版本号；无记录视为 0。
     * Redis 读失败时优先用本地缓存（含未过期），否则回落 0（签发侧不阻断登录）。
     *
     * @param type 主体类型
     * @param id   主体 ID
     * @return 版本
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public long current(PrincipalType type, Long id) {
        if (type == null || id == null) {
            return 0L;
        }
        OptionalLong cached = peekCache(type, id, false);
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return cached.orElse(0L);
        }
        try {
            String raw = redis.opsForValue().get(key(type, id));
            long version = 0L;
            if (StringUtils.hasText(raw)) {
                version = Long.parseLong(raw.trim());
            }
            putCache(type, id, version, CACHE_TTL_MS);
            return version;
        } catch (Exception ex) {
            log.warn("read tokenVersion failed typ={} id={}: {}", type, id, ex.getMessage());
            return peekCache(type, id, true).orElse(0L);
        }
    }

    /**
     * 递增版本，使既有 JWT 全部失效。
     * Redis 不可用时抛出业务异常，调用方应回滚启停/删除，避免「已禁用但仍可访问」。
     *
     * @param type 主体类型
     * @param id   主体 ID
     * @return 新版本
     * @throws BusinessException Redis 不可用或自增失败
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public long bump(PrincipalType type, Long id) {
        if (type == null || id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法吊销登录态：主体无效");
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            log.warn("tokenVersion bump failed: Redis unavailable typ={} id={}", type, id);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法吊销登录态，请稍后重试");
        }
        try {
            Long next = redis.opsForValue().increment(key(type, id));
            if (next == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法吊销登录态，请稍后重试");
            }
            putCache(type, id, next, BUMP_CACHE_TTL_MS);
            return next;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("tokenVersion bump failed typ={} id={}: {}", type, id, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法吊销登录态，请稍后重试");
        }
    }

    /**
     * claim 版本是否仍有效。
     * Redis 读失败时：本地曾有版本记录（含过期）则按该版本 fail-closed；否则 fail-open。
     *
     * @param type         主体类型
     * @param id           主体 ID
     * @param claimVersion JWT 中的 tv
     * @return true 匹配；或无吊销证据时 fail-open
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public boolean matches(PrincipalType type, Long id, long claimVersion) {
        if (type == null || id == null) {
            return claimVersion == 0L;
        }
        OptionalLong fresh = peekCache(type, id, false);
        if (fresh.isPresent()) {
            return fresh.getAsLong() == claimVersion;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            // 无 Redis：无法维护版本；若本地曾 bump 过则仍按缓存 fail-closed
            OptionalLong stale = peekCache(type, id, true);
            if (stale.isPresent()) {
                return stale.getAsLong() == claimVersion;
            }
            return true;
        }
        try {
            String raw = redis.opsForValue().get(key(type, id));
            long version = 0L;
            if (StringUtils.hasText(raw)) {
                version = Long.parseLong(raw.trim());
            }
            putCache(type, id, version, CACHE_TTL_MS);
            return version == claimVersion;
        } catch (Exception ex) {
            OptionalLong stale = peekCache(type, id, true);
            if (stale.isPresent()) {
                log.warn("match tokenVersion failed (fail-closed via cache) typ={} id={}: {}",
                        type, id, ex.getMessage());
                return stale.getAsLong() == claimVersion;
            }
            log.warn("match tokenVersion failed (fail-open) typ={} id={}: {}", type, id, ex.getMessage());
            return true;
        }
    }

    /**
     * @param allowExpired true 时返回已过期条目（Redis 故障时作吊销证据）
     */
    private OptionalLong peekCache(PrincipalType type, Long id, boolean allowExpired) {
        CacheEntry entry = localCache.get(cacheKey(type, id));
        if (entry == null) {
            return OptionalLong.empty();
        }
        if (!allowExpired && entry.expireAtMillis < System.currentTimeMillis()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(entry.version);
    }

    /**
     * 单调写入缓存：不允许用更低版本覆盖更高版本（防并发读回写）。
     */
    private void putCache(PrincipalType type, Long id, long version, long ttlMs) {
        long expireAt = System.currentTimeMillis() + Math.max(1L, ttlMs);
        localCache.compute(cacheKey(type, id), (k, old) -> {
            if (old != null && old.version > version) {
                // 保留更高版本，并延长其存活，避免吊销信息被读路径冲掉
                return new CacheEntry(old.version, Math.max(old.expireAtMillis, expireAt));
            }
            return new CacheEntry(version, expireAt);
        });
    }

    private static String key(PrincipalType type, Long id) {
        return KEY_PREFIX + type.name() + ":" + id;
    }

    private static String cacheKey(PrincipalType type, Long id) {
        return type.name() + ":" + id;
    }

    private record CacheEntry(long version, long expireAtMillis) {
    }
}
