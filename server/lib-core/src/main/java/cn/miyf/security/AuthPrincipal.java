package cn.miyf.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 登录主体：承载用户/管理员 ID、类型与权限码。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
public class AuthPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final PrincipalType type;
    private final List<String> permissions;
    private final boolean enabled;

    /**
     * 构造主体。
     *
     * @param id          主键
     * @param username    展示名/登录名
     * @param type        主体类型
     * @param permissions 权限码列表
     * @param enabled     是否启用
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthPrincipal(Long id, String username, PrincipalType type, List<String> permissions, boolean enabled) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
        this.enabled = enabled;
    }

    /**
     * 获取主体 ID。
     *
     * @return ID
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取主体类型。
     *
     * @return 类型
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public PrincipalType getType() {
        return type;
    }

    /**
     * 获取权限码列表。
     *
     * @return 权限码
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * 是否具备指定权限码。
     *
     * @param permission 权限码
     * @return true 表示具备
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_* 供 hasRole；权限码直接作为 Authority，供 hasAuthority / @RequirePermission 使用
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + type.name()));
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
