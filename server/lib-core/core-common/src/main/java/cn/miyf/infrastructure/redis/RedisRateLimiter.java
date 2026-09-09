package cn.miyf.infrastructure.redis;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 固定窗口计数限流（INCR + EXPIRE）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAppProperties properties;

    
    /**
     * 检查并递增；超限抛 {@link ErrorCode#TOO_MANY_REQUESTS}。
     * Redis 故障时降级放行（适合普通读接口）。
     *
     * @param key           计数键
     * @param maxRequests   窗口上限
     * @param windowSeconds 窗口秒数
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void checkAndIncrement(String key, int maxRequests, long windowSeconds) {
        checkAndIncrement(key, maxRequests, windowSeconds, false);
    }

    /**
     * 严格限流：Redis 故障时拒绝请求（适合写操作 / 上传 / OAuth 等）。
     *
     * @param key           计数键
     * @param maxRequests   窗口上限
     * @param windowSeconds 窗口秒数
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void checkAndIncrementStrict(String key, int maxRequests, long windowSeconds) {
        checkAndIncrement(key, maxRequests, windowSeconds, true);
    }

    /**
     * 固定窗口限流。
     *
     * @param key           计数键
     * @param maxRequests   上限
     * @param windowSeconds 窗口
     * @param strict        true 时 Redis 故障拒绝
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void checkAndIncrement(String key, int maxRequests, long windowSeconds, boolean strict) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return;
        }
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Math.max(windowSeconds, 1), TimeUnit.SECONDS);
            }
            if (count != null && count > maxRequests) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            if (strict) {
                log.error("Redis rate limit failed, reject. key={}", key, ex);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "限流服务暂不可用，请稍后重试");
            }
            log.warn("Redis rate limit failed, allow request. key={}", key, ex);
        }
    }

    /**
     * 登录失败计数；超限抛错。
     * Redis 已启用但不可用时拒绝登录（fail-closed）。
     *
     * @param principal 登录主体标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void assertLoginAllowed(String principal) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = RedisCacheKeys.loginFail(principal);
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw != null) {
                long fails = Long.parseLong(raw);
                if (fails >= properties.getRateLimit().getLoginMaxAttempts()) {
                    throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试过多，请稍后再试");
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Redis login assert failed, reject. principal={}", principal, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录风控服务暂不可用，请稍后重试");
        }
    }

    /**
     * 记录一次登录失败。
     * Redis 已启用但写入失败时抛错，避免失败次数丢失导致限流失效。
     *
     * @param principal 登录主体标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void recordLoginFailure(String principal) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = RedisCacheKeys.loginFail(principal);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(
                        key, properties.getRateLimit().getLoginWindowSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            log.error("Redis record login fail failed. principal={}", principal, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录风控服务暂不可用，请稍后重试");
        }
    }

    /**
     * 登录成功清除失败计数。
     *
     * @param principal 登录主体标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void clearLoginFailures(String principal) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisCacheKeys.loginFail(principal));
        } catch (Exception ex) {
            log.warn("Redis clear login fail failed. principal={}", principal, ex);
        }
    }
}

