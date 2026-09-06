package cn.miyf.common;

import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.LoginUserContext;
import cn.miyf.security.PrincipalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LocalThreadMap / LoginUserContext 单元测试。
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

    @Test
    void loginUserContextStoresPrincipal() {
        AuthPrincipal principal = new AuthPrincipal(
                20001L, "alice", PrincipalType.USER, List.of(), true);
        LoginUserContext.set(principal);
        assertEquals(principal.getId(), LoginUserContext.get().getId());
        LoginUserContext.clear();
        assertNull(LoginUserContext.get());
    }
}
