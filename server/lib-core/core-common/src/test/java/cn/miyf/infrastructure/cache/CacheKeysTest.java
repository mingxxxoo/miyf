package cn.miyf.infrastructure.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CacheKeys 规范单测。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
class CacheKeysTest {

    @Test
    void join_shouldPrefixRootAndDomains() {
        assertEquals("ck:perm:file:access:admin:1:99",
                CacheKeys.join(CacheKeys.DOMAIN_PERM, "file", "access", "admin", 1L, 99L));
        assertEquals("ck:data:cat:enabled",
                CacheKeys.join(CacheKeys.DOMAIN_DATA, "cat", "enabled"));
        assertEquals("ck:auth:login:fail:u",
                CacheKeys.join(CacheKeys.DOMAIN_AUTH, "login", "fail", "u"));
    }
}
