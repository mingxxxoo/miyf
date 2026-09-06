package cn.miyf.infrastructure.redis;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAppProperties properties;

    /**
     * 构造限流组件。
     *
     * @param stringRedisTemplate Redis
     * @param properties          配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public RedisRateLimiter(StringRedisTemplate stringRedisTemplate, RedisAppProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 检查并递增；超限抛 {@link ErrorCode#TOO_MANY_REQUESTS}。
     *
     * @param key           计数键
     * @param maxRequests   窗口上限
     * @param windowSeconds 窗口秒数
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void checkAndIncrement(String key, int maxRequests, long windowSeconds) {
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
            log.warn("Redis rate limit failed, allow request. key={}", key, ex);
        }
    }

    /**
     * 登录失败计数；超限抛错。
     *
     * @param principal 登录主体标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void assertLoginAllowed(String principal) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = CacheKeys.loginFail(principal);
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
            log.warn("Redis login assert failed, allow. principal={}", principal, ex);
        }
    }

    /**
     * 记录一次登录失败。
     *
     * @param principal 登录主体标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void recordLoginFailure(String principal) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = CacheKeys.loginFail(principal);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(
                        key, properties.getRateLimit().getLoginWindowSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            log.warn("Redis record login fail failed. principal={}", principal, ex);
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
            stringRedisTemplate.delete(CacheKeys.loginFail(principal));
        } catch (Exception ex) {
            log.warn("Redis clear login fail failed. principal={}", principal, ex);
        }
    }
}
