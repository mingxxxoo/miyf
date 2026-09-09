package cn.miyf.infrastructure.redis;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 SET NX + Lua 释放的简单分布式锁。
 * <ul>
 *   <li>{@link LockMode#STRICT}：Redis 异常时失败（默认，强一致场景）</li>
 *   <li>{@link LockMode#DEGRADABLE}：Redis 异常时无锁继续（仅明确允许降级的场景）</li>
 * </ul>
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock {

    /**
     * 锁失败策略。
     */
    public enum LockMode {
        /** Redis 故障或加锁异常时抛错，禁止无锁执行。 */
        STRICT,
        /** Redis 故障时降级为无锁执行（需调用方明确接受并发风险）。 */
        DEGRADABLE
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAppProperties properties;

    /**
     * 强锁执行；拿不到锁抛 {@link ErrorCode#LOCK_BUSY}，Redis 异常抛 {@link ErrorCode#INTERNAL_ERROR}。
     *
     * @param lockKey  锁键
     * @param supplier 业务
     * @param <T>      返回类型
     * @return 业务结果
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, properties.getLock().getDefaultLeaseSeconds(), LockMode.STRICT, supplier);
    }

    /**
     * 强锁执行（可指定租约）。
     *
     * @param lockKey      锁键
     * @param leaseSeconds 租约秒数
     * @param supplier     业务
     * @param <T>          返回类型
     * @return 业务结果
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public <T> T executeWithLock(String lockKey, long leaseSeconds, Supplier<T> supplier) {
        return executeWithLock(lockKey, leaseSeconds, LockMode.STRICT, supplier);
    }

    /**
     * 可降级锁：Redis 不可用时无锁继续执行。
     *
     * @param lockKey  锁键
     * @param supplier 业务
     * @param <T>      返回类型
     * @return 业务结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public <T> T executeWithDegradableLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, properties.getLock().getDefaultLeaseSeconds(), LockMode.DEGRADABLE, supplier);
    }

    /**
     * 按模式加锁执行。
     *
     * @param lockKey      锁键
     * @param leaseSeconds 租约秒数
     * @param mode         失败策略
     * @param supplier     业务
     * @param <T>          返回类型
     * @return 业务结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public <T> T executeWithLock(String lockKey, long leaseSeconds, LockMode mode, Supplier<T> supplier) {
        LockMode effective = mode == null ? LockMode.STRICT : mode;
        if (!properties.isEnabled()) {
            if (effective == LockMode.DEGRADABLE) {
                return supplier.get();
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分布式锁需要启用 Redis（严格模式不可无锁执行）");
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
            if (effective == LockMode.DEGRADABLE) {
                log.warn("Redis lock failed, degrade without lock. key={}", lockKey, ex);
                return supplier.get();
            }
            log.error("Redis lock failed, reject execution. key={}", lockKey, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分布式锁服务暂不可用，请稍后重试");
        } finally {
            if (locked) {
                unlock(lockKey, token);
            }
        }
    }

    /**
     * 仅当 token 匹配时释放锁（无 expire 兜底，避免误伤其他实例租约）。
     *
     * @param lockKey 锁键
     * @param token   持有者令牌
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    private void unlock(String lockKey, String token) {
        try {
            RedisAtomicOps.compareAndDelete(stringRedisTemplate, lockKey, token);
        } catch (Exception ex) {
            log.warn("Redis unlock failed. key={}", lockKey, ex);
        }
    }
}
