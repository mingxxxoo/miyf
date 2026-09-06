package cn.miyf.service;

import cn.miyf.service.BaseApplicationService;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.bean.dto.OrgUnitSaveDto;
import cn.miyf.bean.dto.PermGroupSaveDto;
import cn.miyf.bean.dto.SysMenuSaveDto;
import cn.miyf.bean.dto.SysRoleSaveDto;
import cn.miyf.bean.dto.SysUserSaveDto;
import cn.miyf.bean.entity.SysMenuEntity;
import cn.miyf.bean.entity.SysOrgUnitEntity;
import cn.miyf.bean.entity.SysPermGroupEntity;
import cn.miyf.bean.entity.SysPermGroupItemEntity;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysRolePermGroupEntity;
import cn.miyf.bean.entity.SysUserEntity;
import cn.miyf.bean.entity.SysUserRoleEntity;
import cn.miyf.repository.mapper.SysMenuMapper;
import cn.miyf.repository.mapper.SysOrgUnitMapper;
import cn.miyf.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.repository.mapper.SysPermGroupMapper;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysRolePermGroupMapper;
import cn.miyf.repository.mapper.SysUserMapper;
import cn.miyf.repository.mapper.SysUserRoleMapper;
import cn.miyf.bean.vo.SysMenuTreeVo;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IAM 应用服务：菜单树、组织/用户/角色/权限组/菜单 CRUD 骨架。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
public class IamApplicationService extends BaseApplicationService {

    private final SysMenuMapper menuMapper;
    private final SysOrgUnitMapper orgUnitMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造 IAM 服务。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public IamApplicationService(SysMenuMapper menuMapper,
                                 SysOrgUnitMapper orgUnitMapper,
                                 SysUserMapper userMapper,
                                 SysRoleMapper roleMapper,
                                 SysPermissionMapper permissionMapper,
                                 SysPermGroupMapper permGroupMapper,
                                 SysPermGroupItemMapper permGroupItemMapper,
                                 SysRolePermGroupMapper rolePermGroupMapper,
                                 SysUserRoleMapper userRoleMapper,
                                 SnowflakeIdGenerator snowflakeIdGenerator,
                                 PasswordEncoder passwordEncoder) {
        this.menuMapper = menuMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.permGroupMapper = permGroupMapper;
        this.permGroupItemMapper = permGroupItemMapper;
        this.rolePermGroupMapper = rolePermGroupMapper;
        this.userRoleMapper = userRoleMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 当前用户可见菜单树（按 permission_code 过滤；无 code 的 DIR 有可见子节点则保留）。
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

    /**
     * 递归过滤菜单树。
     *
     * @param nodes 节点
     * @param codes 权限码集合
     * @return 过滤后节点
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private List<SysMenuTreeVo> filterMenuTree(List<SysMenuTreeVo> nodes, Set<String> codes) {
        List<SysMenuTreeVo> result = new ArrayList<>();
        for (SysMenuTreeVo node : nodes) {
            List<SysMenuTreeVo> children = filterMenuTree(node.getChildren(), codes);
            node.setChildren(children);
            boolean hasCode = StringUtils.hasText(node.getPermissionCode());
            if (hasCode) {
                if (codes.contains(node.getPermissionCode())) {
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

    // ---------- org-units ----------

    /**
     * 组织单位列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysOrgUnitEntity> listOrgUnits() {
        return orgUnitMapper.selectList(Wrappers.<SysOrgUnitEntity>lambdaQuery()
                .orderByAsc(SysOrgUnitEntity::getSortOrder)
                .orderByAsc(SysOrgUnitEntity::getId));
    }

    /**
     * 创建组织单位。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity createOrgUnit(OrgUnitSaveDto dto) {
        Instant now = Instant.now();
        SysOrgUnitEntity entity = new SysOrgUnitEntity()
                .setParentId(dto.getParentId())
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        orgUnitMapper.insert(entity);
        return entity;
    }

    /**
     * 更新组织单位。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity updateOrgUnit(Long id, OrgUnitSaveDto dto) {
        SysOrgUnitEntity entity = requireOrg(id);
        entity.setParentId(dto.getParentId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        entity.setUpdatedAt(Instant.now());
        orgUnitMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除组织单位。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteOrgUnit(Long id) {
        requireOrg(id);
        orgUnitMapper.deleteById(id);
    }

    private SysOrgUnitEntity requireOrg(Long id) {
        SysOrgUnitEntity entity = orgUnitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织单位不存在");
        }
        return entity;
    }

    // ---------- users ----------

    /**
     * 用户列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysUserEntity> listUsers() {
        return userMapper.selectList(Wrappers.<SysUserEntity>lambdaQuery()
                .orderByDesc(SysUserEntity::getCreatedAt));
    }

    /**
     * 创建用户。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysUserEntity createUser(SysUserSaveDto dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        Instant now = Instant.now();
        SysUserEntity entity = new SysUserEntity()
                .setOrgUnitId(dto.getOrgUnitId())
                .setUsername(dto.getUsername())
                .setPasswordHash(passwordEncoder.encode(dto.getPassword()))
                .setNickname(dto.getNickname())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);
        bindUserRoles(entity.getId(), dto.getRoleIds());
        return entity;
    }

    /**
     * 更新用户。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysUserEntity updateUser(Long id, SysUserSaveDto dto) {
        SysUserEntity entity = requireUser(id);
        entity.setOrgUnitId(dto.getOrgUnitId());
        entity.setUsername(dto.getUsername());
        entity.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setUpdatedAt(Instant.now());
        userMapper.updateById(entity);
        if (dto.getRoleIds() != null) {
            bindUserRoles(id, dto.getRoleIds());
        }
        return entity;
    }

    /**
     * 删除用户。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteUser(Long id) {
        requireUser(id);
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
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
                    .setCreatedAt(now);
            userRoleMapper.insert(bind);
        }
    }

    // ---------- roles ----------

    /**
     * 角色列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysRoleEntity> listRoles() {
        return roleMapper.selectList(Wrappers.<SysRoleEntity>lambdaQuery()
                .orderByAsc(SysRoleEntity::getCode));
    }

    /**
     * 创建角色并可绑定权限组。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysRoleEntity createRole(SysRoleSaveDto dto) {
        Instant now = Instant.now();
        SysRoleEntity entity = new SysRoleEntity()
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setDescription(dto.getDescription());
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        roleMapper.insert(entity);
        bindRoleGroups(entity.getId(), dto.getGroupIds());
        return entity;
    }

    /**
     * 更新角色。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysRoleEntity updateRole(Long id, SysRoleSaveDto dto) {
        SysRoleEntity entity = requireRole(id);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedAt(Instant.now());
        roleMapper.updateById(entity);
        if (dto.getGroupIds() != null) {
            bindRoleGroups(id, dto.getGroupIds());
        }
        return entity;
    }

    /**
     * 删除角色。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        rolePermGroupMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
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
                    .setCreatedAt(now);
            rolePermGroupMapper.insert(bind);
        }
    }

    // ---------- perm-groups ----------

    /**
     * 权限组列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysPermGroupEntity> listPermGroups() {
        return permGroupMapper.selectList(Wrappers.<SysPermGroupEntity>lambdaQuery()
                .orderByAsc(SysPermGroupEntity::getSortOrder)
                .orderByAsc(SysPermGroupEntity::getCode));
    }

    /**
     * 创建权限组。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysPermGroupEntity createPermGroup(PermGroupSaveDto dto) {
        Instant now = Instant.now();
        SysPermGroupEntity entity = new SysPermGroupEntity()
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setDescription(dto.getDescription())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        permGroupMapper.insert(entity);
        bindGroupPermissions(entity.getId(), dto.getPermissionIds());
        return entity;
    }

    /**
     * 更新权限组。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysPermGroupEntity updatePermGroup(Long id, PermGroupSaveDto dto) {
        SysPermGroupEntity entity = requirePermGroup(id);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        entity.setUpdatedAt(Instant.now());
        permGroupMapper.updateById(entity);
        if (dto.getPermissionIds() != null) {
            bindGroupPermissions(id, dto.getPermissionIds());
        }
        return entity;
    }

    /**
     * 删除权限组。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deletePermGroup(Long id) {
        requirePermGroup(id);
        permGroupItemMapper.deleteByGroupId(id);
        permGroupMapper.deleteById(id);
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
                    .setCreatedAt(now);
            permGroupItemMapper.insert(item);
        }
    }

    // ---------- permissions ----------

    /**
     * 权限列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysPermissionEntity> listPermissions() {
        return permissionMapper.selectList(Wrappers.<SysPermissionEntity>lambdaQuery()
                .orderByAsc(SysPermissionEntity::getCode));
    }

    // ---------- menus ----------

    /**
     * 菜单列表（扁平）。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public List<SysMenuEntity> listMenus() {
        return menuMapper.selectList(Wrappers.<SysMenuEntity>lambdaQuery()
                .orderByAsc(SysMenuEntity::getSortOrder)
                .orderByAsc(SysMenuEntity::getId));
    }

    /**
     * 创建菜单。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysMenuEntity createMenu(SysMenuSaveDto dto) {
        Instant now = Instant.now();
        SysMenuEntity entity = fromMenuDto(dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        menuMapper.insert(entity);
        return entity;
    }

    /**
     * 更新菜单。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysMenuEntity updateMenu(Long id, SysMenuSaveDto dto) {
        SysMenuEntity entity = requireMenu(id);
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setIcon(dto.getIcon());
        entity.setMenuType(dto.getMenuType());
        entity.setPermissionCode(dto.getPermissionCode());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getVisible() != null) {
            entity.setVisible(dto.getVisible());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        entity.setUpdatedAt(Instant.now());
        menuMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除菜单。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteMenu(Long id) {
        requireMenu(id);
        menuMapper.deleteById(id);
    }

    private SysMenuEntity requireMenu(Long id) {
        SysMenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        return entity;
    }

    private SysMenuEntity fromMenuDto(SysMenuSaveDto dto) {
        return new SysMenuEntity()
                .setParentId(dto.getParentId())
                .setName(dto.getName())
                .setPath(dto.getPath())
                .setComponent(dto.getComponent())
                .setIcon(dto.getIcon())
                .setMenuType(dto.getMenuType())
                .setPermissionCode(dto.getPermissionCode())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setVisible(dto.getVisible() == null || dto.getVisible())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
    }
}
