package cn.miyf.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 登录主体：承载用户/管理员 ID、类型、权限码与数据范围。
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
    private final Long orgUnitId;
    private final DataScope dataScope;

    /**
     * 兼容旧构造（无组织 / 数据范围为 ALL）。
     */
    public AuthPrincipal(Long id, String username, PrincipalType type, List<String> permissions, boolean enabled) {
        this(id, username, type, permissions, enabled, null, DataScope.ALL);
    }

    /**
     * 构造主体（含所属组织与数据范围）。
     *
     * @param id          主键
     * @param username    展示名/登录名
     * @param type        主体类型
     * @param permissions 权限码列表
     * @param enabled     是否启用
     * @param orgUnitId   所属组织（管理员）
     * @param dataScope   有效数据范围
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthPrincipal(Long id,
                         String username,
                         PrincipalType type,
                         List<String> permissions,
                         boolean enabled,
                         Long orgUnitId,
                         DataScope dataScope) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
        this.enabled = enabled;
        this.orgUnitId = orgUnitId;
        this.dataScope = dataScope == null ? DataScope.ALL : dataScope;
    }

    public Long getId() {
        return id;
    }

    public PrincipalType getType() {
        return type;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
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
