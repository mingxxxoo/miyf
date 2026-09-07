package cn.miyf.service;

import cn.miyf.bean.dto.PermGroupSaveDto;
import cn.miyf.bean.dto.RoleDefaultDto;
import cn.miyf.bean.dto.RoleUsersAddDto;
import cn.miyf.bean.dto.SysMenuSaveDto;
import cn.miyf.bean.dto.SysRoleSaveDto;
import cn.miyf.bean.entity.SysMenuEntity;
import cn.miyf.bean.entity.SysPermGroupEntity;
import cn.miyf.bean.entity.SysPermGroupItemEntity;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysRolePermGroupEntity;
import cn.miyf.bean.entity.SysUserEntity;
import cn.miyf.bean.entity.SysUserRoleEntity;
import cn.miyf.bean.vo.RoleAuthSummaryVo;
import cn.miyf.bean.vo.RoleUserVo;
import cn.miyf.bean.vo.SysMenuTreeVo;
import cn.miyf.bean.vo.SysPermGroupVo;
import cn.miyf.bean.vo.SysRoleVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysMenuMapper;
import cn.miyf.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.repository.mapper.SysPermGroupMapper;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysRolePermGroupMapper;
import cn.miyf.repository.mapper.SysUserMapper;
import cn.miyf.repository.mapper.SysUserRoleMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.DataScope;
import cn.miyf.security.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限域应用服务：菜单、角色、权限组、权限点、当前用户菜单树。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class PermissionApplicationService extends BaseApplicationService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final DataScopeService dataScopeService;

    public PermissionApplicationService(SysMenuMapper menuMapper,
                                        SysRoleMapper roleMapper,
                                        SysPermissionMapper permissionMapper,
                                        SysPermGroupMapper permGroupMapper,
                                        SysPermGroupItemMapper permGroupItemMapper,
                                        SysRolePermGroupMapper rolePermGroupMapper,
                                        SysUserMapper userMapper,
                                        SysUserRoleMapper userRoleMapper,
                                        SnowflakeIdGenerator snowflakeIdGenerator,
                                        DataScopeService dataScopeService) {
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.permGroupMapper = permGroupMapper;
        this.permGroupItemMapper = permGroupItemMapper;
        this.rolePermGroupMapper = rolePermGroupMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 当前用户可见菜单树（自原 IamApplicationService 拆出）。
     *
     * @return 菜单树
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysMenuTreeVo> currentUserMenus() {
        AuthPrincipal principal = SecurityUtils.requireAdmin();
        Set<String> codes = new HashSet<>(principal.getPermissions());
        List<SysMenuEntity> all = menuMapper.selectVisibleMenus();
        Map<Long, SysMenuTreeVo> nodeMap = new HashMap<>();
        for (SysMenuEntity menu : all) {
            nodeMap.put(menu.getId(), toMenuVo(menu));
        }
        List<SysMenuTreeVo> roots = new ArrayList<>();
        for (SysMenuEntity menu : all) {
            SysMenuTreeVo node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId != null && nodeMap.containsKey(parentId)) {
                nodeMap.get(parentId).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return filterMenuTree(roots, codes);
    }

    private List<SysMenuTreeVo> filterMenuTree(List<SysMenuTreeVo> nodes, Set<String> codes) {
        boolean wildcard = codes.contains("*");
        List<SysMenuTreeVo> result = new ArrayList<>();
        for (SysMenuTreeVo node : nodes) {
            List<SysMenuTreeVo> children = filterMenuTree(node.getChildren(), codes);
            node.setChildren(children);
            boolean hasCode = StringUtils.hasText(node.getPermissionCode());
            if (hasCode) {
                if (wildcard || codes.contains(node.getPermissionCode())) {
                    result.add(node);
                }
            } else if ("DIR".equals(node.getMenuType())) {
                if (!children.isEmpty()) {
                    result.add(node);
                }
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private SysMenuTreeVo toMenuVo(SysMenuEntity menu) {
        return new SysMenuTreeVo()
                .setId(menu.getId())
                .setParentId(menu.getParentId())
                .setName(menu.getName())
                .setPath(menu.getPath())
                .setComponent(menu.getComponent())
                .setIcon(menu.getIcon())
                .setMenuType(menu.getMenuType())
                .setPermissionCode(menu.getPermissionCode())
                .setSortOrder(menu.getSortOrder())
                .setChildren(new ArrayList<>());
    }

    public List<SysRoleVo> listRoles(String product) {
        var query = Wrappers.<SysRoleEntity>lambdaQuery().orderByAsc(SysRoleEntity::getCode);
        if (StringUtils.hasText(product)) {
            query.eq(SysRoleEntity::getProduct, product.trim());
        }
        return roleMapper.selectList(query).stream().map(this::toRoleVo).toList();
    }

    public RoleAuthSummaryVo authSummary(String product) {
        List<SysRoleVo> roles = listRoles(product);
        Set<Long> userIds = new HashSet<>();
        for (SysRoleVo role : roles) {
            List<SysUserRoleEntity> binds = userRoleMapper.selectList(
                    Wrappers.<SysUserRoleEntity>lambdaQuery().eq(SysUserRoleEntity::getRoleId, role.getId()));
            for (SysUserRoleEntity bind : binds) {
                userIds.add(bind.getUserId());
            }
        }
        return new RoleAuthSummaryVo().setRoleCount(roles.size()).setUserCount(userIds.size());
    }

    @Transactional
    public SysRoleVo createRole(SysRoleSaveDto dto) {
        Instant now = Instant.now();
        SysRoleEntity entity = new SysRoleEntity()
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setDescription(dto.getDescription())
                .setProduct(normalizeProduct(dto.getProduct()))
                .setDataScope(normalizeDataScope(dto.getDataScope()))
                .setIsDefault(false);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        roleMapper.insert(entity);
        bindRoleGroups(entity.getId(), parseIds(dto.getGroupIds()));
        return toRoleVo(entity);
    }

    @Transactional
    public SysRoleVo updateRole(Long id, SysRoleSaveDto dto) {
        SysRoleEntity entity = requireRole(id);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getProduct())) {
            entity.setProduct(normalizeProduct(dto.getProduct()));
        }
        if (StringUtils.hasText(dto.getDataScope())) {
            entity.setDataScope(normalizeDataScope(dto.getDataScope()));
        }
        entity.setLastModifyTime(Instant.now());
        roleMapper.updateById(entity);
        if (dto.getGroupIds() != null) {
            bindRoleGroups(id, parseIds(dto.getGroupIds()));
        }
        return toRoleVo(entity);
    }

    @Transactional
    public SysRoleVo setDefaultRole(Long id, RoleDefaultDto dto) {
        SysRoleEntity entity = requireRole(id);
        if (dto.isDefault()) {
            var defaultQuery = Wrappers.<SysRoleEntity>lambdaQuery().eq(SysRoleEntity::getIsDefault, true);
            if (StringUtils.hasText(entity.getProduct())) {
                defaultQuery.eq(SysRoleEntity::getProduct, entity.getProduct());
            }
            List<SysRoleEntity> defaults = roleMapper.selectList(defaultQuery);
            Instant now = Instant.now();
            for (SysRoleEntity other : defaults) {
                if (!Objects.equals(other.getId(), id)) {
                    other.setIsDefault(false);
                    other.setLastModifyTime(now);
                    roleMapper.updateById(other);
                }
            }
            entity.setIsDefault(true);
        } else {
            entity.setIsDefault(false);
        }
        entity.setLastModifyTime(Instant.now());
        roleMapper.updateById(entity);
        return toRoleVo(entity);
    }

    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        rolePermGroupMapper.deleteByRoleId(id);
        userRoleMapper.delete(Wrappers.<SysUserRoleEntity>lambdaQuery().eq(SysUserRoleEntity::getRoleId, id));
        roleMapper.deleteById(id);
    }

    public List<RoleUserVo> listRoleUsers(Long roleId) {
        requireRole(roleId);
        AuthPrincipal principal = dataScopeService.requireAdmin();
        List<SysUserRoleEntity> binds = userRoleMapper.selectList(
                Wrappers.<SysUserRoleEntity>lambdaQuery().eq(SysUserRoleEntity::getRoleId, roleId));
        if (binds.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = binds.stream().map(SysUserRoleEntity::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(Wrappers.<SysUserEntity>lambdaQuery().in(SysUserEntity::getId, userIds)).stream()
                .filter(u -> dataScopeService.canAccessUser(principal, u.getId(), u.getOrgUnitId()))
                .map(u -> new RoleUserVo()
                        .setId(u.getId())
                        .setUsername(u.getUsername())
                        .setNickname(u.getNickname())
                        .setStatus(u.getStatus()))
                .toList();
    }

    @Transactional
    public void addRoleUsers(Long roleId, RoleUsersAddDto dto) {
        requireRole(roleId);
        AuthPrincipal principal = dataScopeService.requireAdmin();
        List<Long> userIds = parseIds(dto.getUserIds());
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (Long userId : userIds) {
            SysUserEntity user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + userId);
            }
            dataScopeService.assertCanAccessUser(principal, user.getId(), user.getOrgUnitId());
            Long exists = userRoleMapper.selectCount(Wrappers.<SysUserRoleEntity>lambdaQuery()
                    .eq(SysUserRoleEntity::getRoleId, roleId)
                    .eq(SysUserRoleEntity::getUserId, userId));
            if (exists != null && exists > 0) {
                continue;
            }
            SysUserRoleEntity bind = new SysUserRoleEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setUserId(userId)
                    .setRoleId(roleId)
                    .setCreateTime(now);
            userRoleMapper.insert(bind);
        }
    }

    @Transactional
    public void removeRoleUser(Long roleId, Long userId) {
        requireRole(roleId);
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysUserEntity user = userMapper.selectById(userId);
        if (user != null) {
            dataScopeService.assertCanAccessUser(principal, user.getId(), user.getOrgUnitId());
        }
        userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
    }

    public byte[] exportRoleUsersCsv(Long roleId) {
        SysRoleEntity role = requireRole(roleId);
        List<RoleUserVo> users = listRoleUsers(roleId);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("roleId,roleCode,roleName,userId,username,nickname,status\n");
        for (RoleUserVo u : users) {
            sb.append(csv(role.getId())).append(',')
                    .append(csv(role.getCode())).append(',')
                    .append(csv(role.getName())).append(',')
                    .append(csv(u.getId())).append(',')
                    .append(csv(u.getUsername())).append(',')
                    .append(csv(u.getNickname())).append(',')
                    .append(csv(u.getStatus())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private SysRoleVo toRoleVo(SysRoleEntity entity) {
        List<SysRolePermGroupEntity> binds = rolePermGroupMapper.selectList(
                Wrappers.<SysRolePermGroupEntity>lambdaQuery().eq(SysRolePermGroupEntity::getRoleId, entity.getId()));
        List<String> groupIds = binds.stream()
                .map(SysRolePermGroupEntity::getGroupId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        int userCount = userRoleMapper.countByRoleId(entity.getId());
        return new SysRoleVo()
                .setId(entity.getId())
                .setCode(entity.getCode())
                .setName(entity.getName())
                .setDescription(entity.getDescription())
                .setIsDefault(Boolean.TRUE.equals(entity.getIsDefault()))
                .setProduct(StringUtils.hasText(entity.getProduct()) ? entity.getProduct() : "system")
                .setDataScope(normalizeDataScope(entity.getDataScope()))
                .setUserCount(userCount)
                .setGroupIds(new ArrayList<>(groupIds))
                .setCreateTime(entity.getCreateTime())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    private static String normalizeProduct(String product) {
        if (!StringUtils.hasText(product)) {
            return "system";
        }
        String p = product.trim().toLowerCase();
        return switch (p) {
            case "kitchen", "health", "system", "iam" -> "iam".equals(p) ? "system" : p;
            default -> p;
        };
    }

    private static String normalizeDataScope(String dataScope) {
        return DataScope.parse(dataScope).name();
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    public List<SysPermGroupVo> listPermGroups() {
        List<SysPermGroupEntity> groups = permGroupMapper.selectList(Wrappers.<SysPermGroupEntity>lambdaQuery()
                .orderByAsc(SysPermGroupEntity::getSortOrder)
                .orderByAsc(SysPermGroupEntity::getCode));
        return toPermGroupVos(groups);
    }

    @Transactional
    public SysPermGroupVo createPermGroup(PermGroupSaveDto dto) {
        Instant now = Instant.now();
        SysPermGroupEntity entity = new SysPermGroupEntity()
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setDescription(dto.getDescription())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setProduct(StringUtils.hasText(dto.getProduct()) ? dto.getProduct().trim() : "system");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        permGroupMapper.insert(entity);
        bindGroupPermissions(entity.getId(), parseIds(dto.getPermissionIds()));
        return toPermGroupVo(entity);
    }

    @Transactional
    public SysPermGroupVo updatePermGroup(Long id, PermGroupSaveDto dto) {
        SysPermGroupEntity entity = requirePermGroup(id);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getProduct() != null) {
            entity.setProduct(StringUtils.hasText(dto.getProduct()) ? dto.getProduct().trim() : "system");
        }
        entity.setLastModifyTime(Instant.now());
        permGroupMapper.updateById(entity);
        if (dto.getPermissionIds() != null) {
            bindGroupPermissions(id, parseIds(dto.getPermissionIds()));
        }
        return toPermGroupVo(entity);
    }

    @Transactional
    public void deletePermGroup(Long id) {
        requirePermGroup(id);
        rolePermGroupMapper.deleteByGroupId(id);
        permGroupItemMapper.deleteByGroupId(id);
        permGroupMapper.deleteById(id);
    }

    private List<SysPermGroupVo> toPermGroupVos(List<SysPermGroupEntity> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(SysPermGroupEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, List<String>> permIdsByGroup = new HashMap<>();
        if (!groupIds.isEmpty()) {
            List<SysPermGroupItemEntity> items = permGroupItemMapper.selectByGroupIds(groupIds);
            for (SysPermGroupItemEntity item : items) {
                if (item.getGroupId() == null || item.getPermissionId() == null) {
                    continue;
                }
                permIdsByGroup
                        .computeIfAbsent(item.getGroupId(), k -> new ArrayList<>())
                        .add(String.valueOf(item.getPermissionId()));
            }
        }
        List<SysPermGroupVo> vos = new ArrayList<>(groups.size());
        for (SysPermGroupEntity g : groups) {
            vos.add(toPermGroupVo(g, permIdsByGroup.getOrDefault(g.getId(), List.of())));
        }
        return vos;
    }

    private SysPermGroupVo toPermGroupVo(SysPermGroupEntity entity) {
        List<Long> ids = permGroupItemMapper.selectPermissionIdsByGroupId(entity.getId());
        List<String> permissionIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        return toPermGroupVo(entity, permissionIds);
    }

    private SysPermGroupVo toPermGroupVo(SysPermGroupEntity entity, List<String> permissionIds) {
        return new SysPermGroupVo()
                .setId(entity.getId())
                .setCode(entity.getCode())
                .setName(entity.getName())
                .setDescription(entity.getDescription())
                .setSortOrder(entity.getSortOrder())
                .setProduct(entity.getProduct())
                .setPermissionIds(permissionIds == null ? new ArrayList<>() : new ArrayList<>(permissionIds))
                .setCreateTime(entity.getCreateTime())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    public List<SysPermissionEntity> listPermissions() {
        return permissionMapper.selectList(Wrappers.<SysPermissionEntity>lambdaQuery()
                .orderByAsc(SysPermissionEntity::getCode));
    }

    public List<SysMenuEntity> listMenus() {
        return menuMapper.selectList(Wrappers.<SysMenuEntity>lambdaQuery()
                .orderByAsc(SysMenuEntity::getSortOrder)
                .orderByAsc(SysMenuEntity::getId));
    }

    @Transactional
    public SysMenuEntity createMenu(SysMenuSaveDto dto) {
        Instant now = Instant.now();
        SysMenuEntity entity = fromMenuDto(dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        menuMapper.insert(entity);
        return entity;
    }

    @Transactional
    public SysMenuEntity updateMenu(Long id, SysMenuSaveDto dto) {
        SysMenuEntity entity = requireMenu(id);
        Long parentId = parseId(dto.getParentId());
        assertMenuParentNotCycle(id, parentId);
        entity.setParentId(parentId);
        entity.setName(dto.getName());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setIcon(dto.getIcon());
        entity.setMenuType(dto.getMenuType());
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setProduct(resolveMenuProduct(dto.getProduct(), parentId));
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getVisible() != null) {
            entity.setVisible(dto.getVisible());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        entity.setLastModifyTime(Instant.now());
        menuMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public void deleteMenu(Long id) {
        requireMenu(id);
        menuMapper.deleteById(id);
    }

    private SysRoleEntity requireRole(Long id) {
        SysRoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return entity;
    }

    private void bindRoleGroups(Long roleId, List<Long> groupIds) {
        rolePermGroupMapper.deleteByRoleId(roleId);
        if (groupIds == null) {
            return;
        }
        Instant now = Instant.now();
        for (Long groupId : groupIds) {
            SysRolePermGroupEntity bind = new SysRolePermGroupEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setRoleId(roleId)
                    .setGroupId(groupId)
                    .setCreateTime(now);
            rolePermGroupMapper.insert(bind);
        }
    }

    private SysPermGroupEntity requirePermGroup(Long id) {
        SysPermGroupEntity entity = permGroupMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限组不存在");
        }
        return entity;
    }

    private void bindGroupPermissions(Long groupId, List<Long> permissionIds) {
        permGroupItemMapper.deleteByGroupId(groupId);
        if (permissionIds == null) {
            return;
        }
        Instant now = Instant.now();
        for (Long permissionId : permissionIds) {
            SysPermGroupItemEntity item = new SysPermGroupItemEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setGroupId(groupId)
                    .setPermissionId(permissionId)
                    .setCreateTime(now);
            permGroupItemMapper.insert(item);
        }
    }

    private SysMenuEntity requireMenu(Long id) {
        SysMenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        return entity;
    }

    /**
     * 禁止将父级设为自身或子孙节点，避免菜单树成环。
     */
    private void assertMenuParentNotCycle(Long menuId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(menuId, parentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "父级菜单不能是自身");
        }
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null && guard++ < 1000) {
            if (Objects.equals(cursor, menuId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父级菜单不能选择自身的子节点");
            }
            SysMenuEntity parent = menuMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private SysMenuEntity fromMenuDto(SysMenuSaveDto dto) {
        Long parentId = parseId(dto.getParentId());
        return new SysMenuEntity()
                .setParentId(parentId)
                .setName(dto.getName())
                .setPath(dto.getPath())
                .setComponent(dto.getComponent())
                .setIcon(dto.getIcon())
                .setMenuType(dto.getMenuType())
                .setPermissionCode(dto.getPermissionCode())
                .setProduct(resolveMenuProduct(dto.getProduct(), parentId))
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setVisible(dto.getVisible() == null || dto.getVisible())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
    }

    private String resolveMenuProduct(String raw, Long parentId) {
        if (StringUtils.hasText(raw)) {
            return raw.trim().toLowerCase(Locale.ROOT);
        }
        if (parentId != null) {
            SysMenuEntity parent = menuMapper.selectById(parentId);
            if (parent != null && StringUtils.hasText(parent.getProduct())) {
                return parent.getProduct().trim().toLowerCase(Locale.ROOT);
            }
        }
        return "system";
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
