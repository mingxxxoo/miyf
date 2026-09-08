package cn.miyf.permission.security;

import cn.miyf.auth.security.AdminAuthAuthorityLoader;
import cn.miyf.auth.security.DataScope;
import cn.miyf.permission.bean.entity.SysPermissionEntity;
import cn.miyf.permission.bean.entity.SysRoleEntity;
import cn.miyf.permission.repository.mapper.SysPermissionMapper;
import cn.miyf.permission.repository.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 从 IAM 表加载管理员角色与权限码。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class AdminAuthAuthorityLoaderImpl implements AdminAuthAuthorityLoader {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public AdminAuthAuthorityLoaderImpl(SysRoleMapper sysRoleMapper,
                                        SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<String> loadRoleCodes(Long userId) {
        return sysRoleMapper.findByUserId(userId).stream()
                .map(SysRoleEntity::getCode)
                .toList();
    }

    @Override
    public List<String> loadPermissionCodes(Long userId) {
        return sysPermissionMapper.findByUserId(userId).stream()
                .map(SysPermissionEntity::getCode)
                .toList();
    }

    @Override
    public String loadEffectiveDataScope(Long userId) {
        List<DataScope> scopes = sysRoleMapper.findByUserId(userId).stream()
                .map(r -> DataScope.parse(r.getDataScope()))
                .toList();
        return DataScope.widest(scopes).name();
    }
}
