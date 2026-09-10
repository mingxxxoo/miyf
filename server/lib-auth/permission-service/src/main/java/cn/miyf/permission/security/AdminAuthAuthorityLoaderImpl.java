package cn.miyf.permission.security;

import cn.miyf.auth.bean.entity.SysUserRoleEntity;
import cn.miyf.auth.repository.mapper.SysUserRoleMapper;
import cn.miyf.auth.security.AdminAuthAuthorityLoader;
import cn.miyf.auth.security.DataScope;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.permission.bean.entity.SysPermissionEntity;
import cn.miyf.permission.bean.entity.SysRoleEntity;
import cn.miyf.permission.repository.mapper.SysPermissionMapper;
import cn.miyf.permission.repository.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 从 IAM 表加载管理员角色与权限码。
 * 登录时若用户无角色，自动绑定各产品域个人默认角色（{@code is_default}）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthAuthorityLoaderImpl implements AdminAuthAuthorityLoader {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

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

    /**
     * 用户无任何角色时绑定 {@code is_default} 个人默认角色。
     *
     * @param userId 用户 ID
     * @return 是否发生了绑定
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Override
    @Transactional
    public boolean ensureDefaultRolesIfAbsent(Long userId) {
        if (userId == null) {
            return false;
        }
        Long existing = userRoleMapper.selectCount(Wrappers.<SysUserRoleEntity>lambdaQuery()
                .eq(SysUserRoleEntity::getUserId, userId));
        if (existing != null && existing > 0) {
            return false;
        }
        List<SysRoleEntity> defaults = sysRoleMapper.selectList(
                Wrappers.<SysRoleEntity>lambdaQuery().eq(SysRoleEntity::getIsDefault, true));
        if (defaults.isEmpty()) {
            return false;
        }
        Instant now = Instant.now();
        for (SysRoleEntity role : defaults) {
            if (role.getId() == null) {
                continue;
            }
            SysUserRoleEntity bind = new SysUserRoleEntity()
                    .setUserId(userId)
                    .setRoleId(role.getId());
            bind.setId(snowflakeIdGenerator.nextId());
            bind.setCreateTime(now);
            userRoleMapper.insert(bind);
        }
        return true;
    }
}
