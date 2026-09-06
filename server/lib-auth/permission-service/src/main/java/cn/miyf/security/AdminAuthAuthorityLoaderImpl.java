package cn.miyf.security;

import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
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
}
