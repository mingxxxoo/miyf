package cn.miyf.auth.security;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Locale;

/**
 * 角色数据范围（多角色取最宽）。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
public enum DataScope {

    /** 不限制 */
    ALL,
    /** 本组织及下级 */
    ORG_CHILD,
    /** 仅本组织 */
    ORG,
    /** 仅本人 */
    SELF;

    /**
     * 解析字符串，非法值按 ALL。
     */
    public static DataScope parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ALL;
        }
        try {
            return DataScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ALL;
        }
    }

    /**
     * 多角色取最宽范围：ALL &gt; ORG_CHILD &gt; ORG &gt; SELF。
     */
    public static DataScope widest(Collection<DataScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return ALL;
        }
        DataScope best = SELF;
        for (DataScope scope : scopes) {
            if (scope == null) {
                continue;
            }
            if (scope.ordinal() < best.ordinal()) {
                best = scope;
            }
        }
        return best;
    }

    public boolean isAtLeastAsWideAs(DataScope other) {
        if (other == null) {
            return true;
        }
        return this.ordinal() <= other.ordinal();
    }
}
