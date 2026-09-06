package cn.miyf.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LocalThreadMap 单元测试。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:25
 */
class LocalThreadMapTest {

    @AfterEach
    void tearDown() {
        LocalThreadMap.clear();
    }

    @Test
    void putAndGetTypedValue() {
        LocalThreadMap.put("k", "v");
        assertEquals("v", LocalThreadMap.get("k", String.class));
        assertNull(LocalThreadMap.get("k", Integer.class));
        assertTrue(LocalThreadMap.contains("k"));
    }
}
