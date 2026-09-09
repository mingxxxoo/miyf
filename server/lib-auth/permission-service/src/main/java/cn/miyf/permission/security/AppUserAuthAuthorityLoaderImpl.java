package cn.miyf.permission.security;

import cn.miyf.auth.security.AppUserAuthAuthorityLoader;
import cn.miyf.auth.security.DataScope;
import cn.miyf.permission.bean.entity.SysPermissionEntity;
import cn.miyf.permission.bean.entity.SysRoleEntity;
import cn.miyf.permission.repository.mapper.SysPermissionMapper;
import cn.miyf.permission.repository.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从 IAM 默认角色（{@code is_default}）加载用户端角色与权限码。
 * <p>
 * 厨房 / 健康等产品域的 {@code default_person} 由其绑定的「个人」权限组展开为 API 码，供微信登录写入 JWT。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Component
@RequiredArgsConstructor
public class AppUserAuthAuthorityLoaderImpl implements AppUserAuthAuthorityLoader {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<String> loadDefaultRoleCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (SysRoleEntity role : listDefaultRoles()) {
            if (role.getCode() != null && !role.getCode().isBlank()) {
                codes.add(role.getCode());
            }
        }
        return new ArrayList<>(codes);
    }

    @Override
    public List<String> loadDefaultPermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (SysRoleEntity role : listDefaultRoles()) {
            if (role.getId() == null) {
                continue;
            }
            for (SysPermissionEntity perm : sysPermissionMapper.findByRoleId(role.getId())) {
                if (perm.getCode() != null && !perm.getCode().isBlank()) {
                    codes.add(perm.getCode());
                }
            }
        }
        return new ArrayList<>(codes);
    }

    @Override
    public String loadDefaultDataScope() {
        List<DataScope> scopes = listDefaultRoles().stream()
                .map(r -> DataScope.parse(r.getDataScope()))
                .toList();
        return DataScope.widest(scopes).name();
    }

    private List<SysRoleEntity> listDefaultRoles() {
        return sysRoleMapper.selectList(
                Wrappers.<SysRoleEntity>lambdaQuery().eq(SysRoleEntity::getIsDefault, true));
    }
}
