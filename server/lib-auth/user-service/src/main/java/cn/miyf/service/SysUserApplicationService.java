package cn.miyf.service;

import cn.miyf.bean.dto.SysUserSaveDto;
import cn.miyf.bean.entity.SysUserEntity;
import cn.miyf.bean.entity.SysUserRoleEntity;
import cn.miyf.bean.vo.SysUserVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysUserMapper;
import cn.miyf.repository.mapper.SysUserRoleMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.DataScope;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户（管理员账号）应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class SysUserApplicationService extends BaseApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeService dataScopeService;

    public SysUserApplicationService(SysUserMapper userMapper,
                                     SysUserRoleMapper userRoleMapper,
                                     SnowflakeIdGenerator snowflakeIdGenerator,
                                     PasswordEncoder passwordEncoder,
                                     DataScopeService dataScopeService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.passwordEncoder = passwordEncoder;
        this.dataScopeService = dataScopeService;
    }

    public List<SysUserVo> listUsers() {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        List<SysUserEntity> users = userMapper.selectList(Wrappers.<SysUserEntity>lambdaQuery()
                .orderByDesc(SysUserEntity::getCreateTime));
        users = filterByDataScope(principal, users);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUserEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, List<String>> roleIdsByUser = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUserRoleEntity> binds = userRoleMapper.selectList(
                    Wrappers.<SysUserRoleEntity>lambdaQuery().in(SysUserRoleEntity::getUserId, userIds));
            for (SysUserRoleEntity bind : binds) {
                if (bind.getUserId() == null || bind.getRoleId() == null) {
                    continue;
                }
                roleIdsByUser
                        .computeIfAbsent(bind.getUserId(), k -> new ArrayList<>())
                        .add(String.valueOf(bind.getRoleId()));
            }
        }
        return users.stream()
                .map(u -> toUserVo(u, roleIdsByUser.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SysUserVo createUser(SysUserSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        Long orgUnitId = parseId(dto.getOrgUnitId());
        dataScopeService.assertCanAssignOrg(principal, orgUnitId);
        Instant now = Instant.now();
        SysUserEntity entity = new SysUserEntity()
                .setOrgUnitId(orgUnitId)
                .setUsername(dto.getUsername())
                .setPasswordHash(passwordEncoder.encode(dto.getPassword()))
                .setNickname(dto.getNickname())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        userMapper.insert(entity);
        bindUserRoles(entity.getId(), parseIds(dto.getRoleIds()));
        return toUserVo(entity);
    }

    @Transactional
    public SysUserVo updateUser(Long id, SysUserSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity entity = requireUser(id);
        dataScopeService.assertCanAccessUser(principal, entity.getId(), entity.getOrgUnitId());
        Long orgUnitId = parseId(dto.getOrgUnitId());
        dataScopeService.assertCanAssignOrg(principal, orgUnitId);
        entity.setOrgUnitId(orgUnitId);
        entity.setUsername(dto.getUsername());
        entity.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setLastModifyTime(Instant.now());
        userMapper.updateById(entity);
        if (dto.getRoleIds() != null) {
            bindUserRoles(id, parseIds(dto.getRoleIds()));
        }
        return toUserVo(entity);
    }

    @Transactional
    public void deleteUser(Long id) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity entity = requireUser(id);
        dataScopeService.assertCanAccessUser(principal, entity.getId(), entity.getOrgUnitId());
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    private List<SysUserEntity> filterByDataScope(AuthPrincipal principal, List<SysUserEntity> users) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return users;
        }
        if (scope == DataScope.SELF) {
            return users.stream().filter(u -> Objects.equals(u.getId(), principal.getId())).toList();
        }
        Set<Long> allowedOrgs = dataScopeService.resolveAllowedOrgIds(principal);
        if (allowedOrgs == null || allowedOrgs.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .filter(u -> u.getOrgUnitId() != null && allowedOrgs.contains(u.getOrgUnitId()))
                .toList();
    }

    private SysUserVo toUserVo(SysUserEntity entity) {
        List<SysUserRoleEntity> binds = userRoleMapper.selectList(
                Wrappers.<SysUserRoleEntity>lambdaQuery().eq(SysUserRoleEntity::getUserId, entity.getId()));
        List<String> roleIds = binds.stream()
                .map(SysUserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
        return toUserVo(entity, roleIds);
    }

    private SysUserVo toUserVo(SysUserEntity entity, List<String> roleIds) {
        return new SysUserVo()
                .setId(entity.getId())
                .setOrgUnitId(entity.getOrgUnitId())
                .setUsername(entity.getUsername())
                .setNickname(entity.getNickname())
                .setStatus(entity.getStatus())
                .setRoleIds(roleIds == null ? new ArrayList<>() : new ArrayList<>(roleIds))
                .setCreateTime(entity.getCreateTime())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    private SysUserEntity requireUser(Long id) {
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return entity;
    }

    private void bindUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        Instant now = Instant.now();
        for (Long roleId : roleIds) {
            SysUserRoleEntity bind = new SysUserRoleEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setUserId(userId)
                    .setRoleId(roleId)
                    .setCreateTime(now);
            userRoleMapper.insert(bind);
        }
    }

    private Long parseId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ID 格式无效: " + raw);
        }
    }

    private List<Long> parseIds(List<String> raws) {
        if (raws == null) {
            return null;
        }
        return raws.stream().map(this::parseId).filter(Objects::nonNull).toList();
    }
}
