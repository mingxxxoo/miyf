package cn.miyf.infrastructure.redis;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 SET NX + Lua 释放的简单分布式锁。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAppProperties properties;

    /**
     * 构造锁组件。
     *
     * @param stringRedisTemplate Redis
     * @param properties          配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, RedisAppProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 尝试加锁执行；拿不到锁抛 {@link ErrorCode#LOCK_BUSY}。
     *
     * @param lockKey  锁键
     * @param supplier 业务
     * @param <T>      返回类型
     * @return 业务结果
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, properties.getLock().getDefaultLeaseSeconds(), supplier);
    }

    /**
     * 尝试加锁执行。
     *
     * @param lockKey      锁键
     * @param leaseSeconds 租约秒数
     * @param supplier     业务
     * @param <T>          返回类型
     * @return 业务结果
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T executeWithLock(String lockKey, long leaseSeconds, Supplier<T> supplier) {
        if (!properties.isEnabled()) {
            return supplier.get();
        }
        String token = UUID.randomUUID().toString();
        boolean locked = false;
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, Duration.ofSeconds(Math.max(leaseSeconds, 1)));
            locked = Boolean.TRUE.equals(ok);
            if (!locked) {
                throw new BusinessException(ErrorCode.LOCK_BUSY);
            }
            return supplier.get();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis lock failed, execute without lock. key={}", lockKey, ex);
            return supplier.get();
        } finally {
            if (locked) {
                unlock(lockKey, token);
            }
        }
    }

    /**
     * 仅当 token 匹配时释放锁。
     *
     * @param lockKey 锁键
     * @param token   持有者令牌
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (Exception ex) {
            log.warn("Redis unlock failed. key={}", lockKey, ex);
            try {
                stringRedisTemplate.expire(lockKey, 1, TimeUnit.SECONDS);
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}

