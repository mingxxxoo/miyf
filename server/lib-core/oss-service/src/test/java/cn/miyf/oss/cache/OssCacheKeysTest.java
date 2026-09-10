package cn.miyf.oss.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OssCacheKeys 单测。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
class OssCacheKeysTest {

    @Test
    void fileAccess_shouldFollowPermDomain() {
        String key = OssCacheKeys.fileAccess("admin", 1L, 99L);
        assertEquals("ck:perm:file:access:admin:1:99", key);
        assertTrue(key.startsWith(OssCacheKeys.fileAccessPrefix()));
    }

    @Test
    void fileAccessTicket_shouldFollowPermDomain() {
        String key = OssCacheKeys.fileAccessTicket(99L);
        assertEquals("ck:perm:file:ticket:99", key);
        assertTrue(key.startsWith(OssCacheKeys.fileAccessTicketPrefix()));
    }
}
