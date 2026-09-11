package cn.miyf.user.service;

import cn.miyf.auth.bean.entity.SysUserEntity;
import cn.miyf.auth.bean.entity.SysUserRoleEntity;
import cn.miyf.auth.repository.mapper.SysUserMapper;
import cn.miyf.auth.repository.mapper.SysUserRoleMapper;
import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.TokenVersionService;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.organization.service.DataScopeService;
import cn.miyf.permission.bean.entity.SysRoleEntity;
import cn.miyf.permission.repository.mapper.SysRoleMapper;
import cn.miyf.service.BaseApplicationService;
import cn.miyf.user.bean.dto.SysUserSaveDto;
import cn.miyf.user.bean.vo.SysUserVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户（管理员账号）应用服务。
 * 启停转为 DISABLED 或删除时递增 JWT tokenVersion；bump 失败则回滚，避免误判吊销成功。
 * 状态写入统一规范化为 ENABLED/DISABLED。
 *
 * @author XieMingJie
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class SysUserApplicationService extends BaseApplicationService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ENABLED", "DISABLED");

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeService dataScopeService;
    private final TokenVersionService tokenVersionService;

    /**
     * 系统用户列表（按数据范围过滤，附带角色 ID）。
     *
     * @return 用户 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
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

    /**
     * 创建系统用户并绑定角色。
     * <p>
     * 未指定角色时自动绑定各产品域的默认角色（如 {@code default_person}）。
     *
     * @param dto 请求
     * @return 用户 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
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
                .setStatus(StringUtils.hasText(dto.getStatus()) ? normalizeStatus(dto.getStatus()) : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        userMapper.insert(entity);
        List<Long> roleIds = parseIds(dto.getRoleIds());
        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = listDefaultRoleIds();
        }
        bindUserRoles(entity.getId(), roleIds);
        return toUserVo(entity);
    }

    /**
     * 更新系统用户；roleIds 非 null 时重绑角色。
     * 状态与启停接口共用规范化；仅在状态变为 DISABLED 时吊销 JWT。
     *
     * @param id  用户 ID
     * @param dto 请求
     * @return 用户 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public SysUserVo updateUser(Long id, SysUserSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity entity = requireUser(id);
        dataScopeService.assertCanAccessUser(principal, entity.getId(), entity.getOrgUnitId());
        Long orgUnitId = parseId(dto.getOrgUnitId());
        dataScopeService.assertCanAssignOrg(principal, orgUnitId);
        String previousStatus = entity.getStatus();
        entity.setOrgUnitId(orgUnitId);
        entity.setUsername(dto.getUsername());
        entity.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(normalizeStatus(dto.getStatus()));
        }
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setLastModifyTime(Instant.now());
        userMapper.updateById(entity);
        if (dto.getRoleIds() != null) {
            bindUserRoles(id, parseIds(dto.getRoleIds()));
        }
        revokeIfBecameDisabled(PrincipalType.ADMIN, id, previousStatus, entity.getStatus());
        return toUserVo(entity);
    }

    /**
     * 仅更新启停状态，不改角色绑定；转为 DISABLED 时吊销已发 JWT。
     *
     * @param id     用户 ID
     * @param status ENABLED / DISABLED
     * @return VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Transactional
    public SysUserVo updateUserStatus(Long id, String status) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity entity = requireUser(id);
        dataScopeService.assertCanAccessUser(principal, entity.getId(), entity.getOrgUnitId());
        String previousStatus = entity.getStatus();
        String normalized = normalizeStatus(status);
        entity.setStatus(normalized);
        entity.setLastModifyTime(Instant.now());
        userMapper.updateById(entity);
        revokeIfBecameDisabled(PrincipalType.ADMIN, id, previousStatus, normalized);
        return toUserVo(entity);
    }

    /**
     * 删除系统用户（级联用户-角色绑定）；同时吊销 JWT。
     *
     * @param id 用户 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteUser(Long id) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity entity = requireUser(id);
        dataScopeService.assertCanAccessUser(principal, entity.getId(), entity.getOrgUnitId());
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
        tokenVersionService.bump(PrincipalType.ADMIN, id);
    }

    /**
     * 规范化启停状态为 ENABLED / DISABLED。
     *
     * @param status 原始状态
     * @return 规范化值
     */
    private static String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 ENABLED 或 DISABLED");
        }
        return normalized;
    }

    /**
     * 仅当状态由非 DISABLED 转为 DISABLED 时吊销 JWT。
     */
    private void revokeIfBecameDisabled(PrincipalType type, Long id, String previous, String next) {
        boolean wasDisabled = previous != null && "DISABLED".equalsIgnoreCase(previous.trim());
        if ("DISABLED".equals(next) && !wasDisabled) {
            tokenVersionService.bump(type, id);
        }
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
                    .setUserId(userId)
                    .setRoleId(roleId);
            bind.setId(snowflakeIdGenerator.nextId());
            bind.setCreateTime(now);
            userRoleMapper.insert(bind);
        }
    }

    /**
     * 列出各产品域标记为默认的角色 ID（如 kitchen/health 的 default_person）。
     *
     * @return 默认角色 ID 列表
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private List<Long> listDefaultRoleIds() {
        List<SysRoleEntity> defaults = roleMapper.selectList(
                Wrappers.<SysRoleEntity>lambdaQuery().eq(SysRoleEntity::getIsDefault, true));
        Set<Long> ids = new LinkedHashSet<>();
        for (SysRoleEntity role : defaults) {
            if (role.getId() != null) {
                ids.add(role.getId());
            }
        }
        return new ArrayList<>(ids);
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
