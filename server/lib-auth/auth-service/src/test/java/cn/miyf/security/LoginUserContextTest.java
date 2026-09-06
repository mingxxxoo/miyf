package cn.miyf.security;

import cn.miyf.common.LocalThreadMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * LoginUserContext 单元测试。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
class LoginUserContextTest {

    @AfterEach
    void tearDown() {
        LocalThreadMap.clear();
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
