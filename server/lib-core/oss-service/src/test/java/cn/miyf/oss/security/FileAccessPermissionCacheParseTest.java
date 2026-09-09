package cn.miyf.oss.security;

import cn.miyf.infrastructure.cache.NoopCacheClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件 ID 解析与 TTL 单测。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
class FileAccessPermissionCacheParseTest {

    private final FileAccessPermissionCache cache = new FileAccessPermissionCache(new NoopCacheClient());

    @Test
    void parseFileId_shouldAcceptUrlAndPlainId() {
        assertEquals(1938L, cache.parseFileId("http://localhost:8080/r/1938").orElseThrow());
        assertEquals(1938L, cache.parseFileId("/r/1938").orElseThrow());
        assertEquals(1938L, cache.parseFileId("1938").orElseThrow());
        assertTrue(cache.parseFileId("not-a-file").isEmpty());
    }

    @Test
    void randomTtl_shouldAroundFiveHours() {
        long seconds = cache.randomTtl().getSeconds();
        assertTrue(seconds >= 5 * 3600 && seconds < 5 * 3600 + 300);
    }
}
