package cn.miyf.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * Redis 原子读删等通用操作封装。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public final class RedisAtomicOps {

    private RedisAtomicOps() {
    }

    /**
     * 原子消费键值：读取并删除，适合一次性票据（OAuth state 等）。
     *
     * @param redis Redis 模板
     * @param key   键
     * @return 原值；不存在或 blank key 时为 null
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static String getAndDelete(StringRedisTemplate redis, String key) {
        if (redis == null || !StringUtils.hasText(key)) {
            return null;
        }
        return redis.execute(RedisScripts.GET_AND_DELETE, Collections.singletonList(key));
    }

    /**
     * 仅当当前值等于期望 token 时删除键。
     *
     * @param redis         Redis 模板
     * @param key           键
     * @param expectedToken 期望值
     * @return true 表示已删除
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public static boolean compareAndDelete(StringRedisTemplate redis, String key, String expectedToken) {
        if (redis == null || !StringUtils.hasText(key) || !StringUtils.hasText(expectedToken)) {
            return false;
        }
        Long deleted = redis.execute(
                RedisScripts.COMPARE_AND_DELETE,
                Collections.singletonList(key),
                expectedToken);
        return deleted != null && deleted > 0;
    }
}
