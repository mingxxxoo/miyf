package cn.miyf.infrastructure.redis;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 限流与锁基础行为单测（Mock Redis）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@ExtendWith(MockitoExtension.class)
class RedisInfraTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisAppProperties properties;
    private RedisRateLimiter rateLimiter;
    private RedisDistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        properties = new RedisAppProperties();
        properties.setEnabled(true);
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setLoginMaxAttempts(3);
        properties.getLock().setDefaultLeaseSeconds(5);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiter = new RedisRateLimiter(stringRedisTemplate, properties);
        distributedLock = new RedisDistributedLock(stringRedisTemplate, properties);
    }

    @Test
    void checkAndIncrement_shouldRejectWhenExceed() {
        when(valueOperations.increment("k")).thenReturn(121L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> rateLimiter.checkAndIncrement("k", 120, 60));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
    }

    @Test
    void assertLoginAllowed_shouldRejectWhenReachMax() {
        when(valueOperations.get(RedisCacheKeys.loginFail("admin:x"))).thenReturn("3");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> rateLimiter.assertLoginAllowed("admin:x"));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
    }

    @Test
    void executeWithLock_shouldRunWhenAcquired() {
        when(valueOperations.setIfAbsent(eq("lock"), anyString(), any(Duration.class))).thenReturn(true);
        AtomicInteger calls = new AtomicInteger();
        Integer result = distributedLock.executeWithLock("lock", calls::incrementAndGet);
        assertEquals(1, result);
        assertEquals(1, calls.get());
        verify(stringRedisTemplate).execute(any(), any(), any());
    }

    @Test
    void executeWithLock_shouldThrowWhenBusy() {
        when(valueOperations.setIfAbsent(eq("lock"), anyString(), any(Duration.class))).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> distributedLock.executeWithLock("lock", () -> 1));
        assertEquals(ErrorCode.LOCK_BUSY.getCode(), ex.getCode());
    }

    @Test
    void executeWithLock_shouldThrowWhenRedisFailsInStrictMode() {
        when(valueOperations.setIfAbsent(eq("lock"), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis down"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> distributedLock.executeWithLock("lock", () -> 1));
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
    }

    @Test
    void executeWithDegradableLock_shouldRunWhenRedisFails() {
        when(valueOperations.setIfAbsent(eq("lock"), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis down"));
        AtomicInteger calls = new AtomicInteger();
        Integer result = distributedLock.executeWithDegradableLock("lock", calls::incrementAndGet);
        assertEquals(1, result);
        assertEquals(1, calls.get());
    }

    @Test
    void executeWithLock_shouldThrowWhenRedisDisabledInStrictMode() {
        properties.setEnabled(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> distributedLock.executeWithLock("lock", () -> 1));
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
    }

    @Test
    void executeWithDegradableLock_shouldRunWhenRedisDisabled() {
        properties.setEnabled(false);
        AtomicInteger calls = new AtomicInteger();
        Integer result = distributedLock.executeWithDegradableLock("lock", calls::incrementAndGet);
        assertEquals(1, result);
        assertEquals(1, calls.get());
    }
}

