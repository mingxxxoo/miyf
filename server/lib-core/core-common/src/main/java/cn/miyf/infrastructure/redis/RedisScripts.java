package cn.miyf.infrastructure.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 公共 Redis Lua 脚本定义（从 classpath 加载，避免散落在业务类中）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public final class RedisScripts {

    /**
     * 原子 GET + DEL，返回原值；键不存在返回 null。
     */
    public static final RedisScript<String> GET_AND_DELETE = RedisScript.of(
            new ClassPathResource("redis/get_and_delete.lua"),
            String.class
    );

    /**
     * 仅当值等于 token 时删除，返回删除数量（0/1）。
     */
    public static final RedisScript<Long> COMPARE_AND_DELETE = RedisScript.of(
            new ClassPathResource("redis/compare_and_delete.lua"),
            Long.class
    );

    private RedisScripts() {
    }
}
