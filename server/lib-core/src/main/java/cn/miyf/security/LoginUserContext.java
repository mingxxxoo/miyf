package cn.miyf.security;

import cn.miyf.common.LocalThreadMap;

/**
 * 登录用户线程上下文：基于 {@link LocalThreadMap} 存取当前登录主体。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:25
 */
public final class LoginUserContext {

    /** LocalThreadMap 中登录主体的键 */
    public static final String KEY_LOGIN_USER = "LOGIN_USER";

    private LoginUserContext() {
    }

    /**
     * 绑定当前线程登录用户。
     *
     * @param principal 登录主体，null 表示清除
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void set(AuthPrincipal principal) {
        LocalThreadMap.put(KEY_LOGIN_USER, principal);
    }

    /**
     * 获取当前线程登录用户，未登录时返回 null。
     *
     * @return 登录主体
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static AuthPrincipal get() {
        return LocalThreadMap.get(KEY_LOGIN_USER, AuthPrincipal.class);
    }

    /**
     * 清除当前线程登录用户（仅移除登录键，不影响 Map 中其他键）。
     *
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void remove() {
        LocalThreadMap.remove(KEY_LOGIN_USER);
    }

    /**
     * 清空当前线程 LocalThreadMap 全部内容。
     *
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void clear() {
        LocalThreadMap.clear();
    }
}
