package cn.miyf.service;

import cn.miyf.bean.dto.PermGroupSaveDto;
import cn.miyf.bean.dto.SysMenuSaveDto;
import cn.miyf.bean.dto.SysRoleSaveDto;
import cn.miyf.bean.entity.SysMenuEntity;
import cn.miyf.bean.entity.SysPermGroupEntity;
import cn.miyf.bean.entity.SysPermGroupItemEntity;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysRolePermGroupEntity;
import cn.miyf.bean.vo.SysMenuTreeVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysMenuMapper;
import cn.miyf.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.repository.mapper.SysPermGroupMapper;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysRolePermGroupMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public PermissionApplicationService(SysMenuMapper menuMapper,
                                        SysRoleMapper roleMapper,
                                        SysPermissionMapper permissionMapper,
                                        SysPermGroupMapper permGroupMapper,
                                        SysPermGroupItemMapper permGroupItemMapper,
                                        SysRolePermGroupMapper rolePermGroupMapper,
                                        SnowflakeIdGenerator snowflakeIdGenerator) {
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.permGroupMapper = permGroupMapper;
        this.permGroupItemMapper = permGroupItemMapper;
        this.rolePermGroupMapper = rolePermGroupMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 当前用户可见菜单树。
     *
     * @return 菜单树
     * @history 1.00 2026-09-05 XieMingJie Created.
     * @history 1.01 2026-09-06 XieMingJie 从 IamApplicationService 拆出。
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

    public List<SysRoleEntity> listRoles() {
        return roleMapper.selectList(Wrappers.<SysRoleEntity>lambdaQuery()
                .orderByAsc(SysRoleEntity::getCode));
    }

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
        bindRoleGroups(entity.getId(), parseIds(dto.getGroupIds()));
        return entity;
    }

    @Transactional
    public SysRoleEntity updateRole(Long id, SysRoleSaveDto dto) {
        SysRoleEntity entity = requireRole(id);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedAt(Instant.now());
        roleMapper.updateById(entity);
        if (dto.getGroupIds() != null) {
            bindRoleGroups(id, parseIds(dto.getGroupIds()));
        }
        return entity;
    }

    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        rolePermGroupMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    public List<SysPermGroupEntity> listPermGroups() {
        return permGroupMapper.selectList(Wrappers.<SysPermGroupEntity>lambdaQuery()
                .orderByAsc(SysPermGroupEntity::getSortOrder)
                .orderByAsc(SysPermGroupEntity::getCode));
    }

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
        bindGroupPermissions(entity.getId(), parseIds(dto.getPermissionIds()));
        return entity;
    }

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
            bindGroupPermissions(id, parseIds(dto.getPermissionIds()));
        }
        return entity;
    }

    @Transactional
    public void deletePermGroup(Long id) {
        requirePermGroup(id);
        permGroupItemMapper.deleteByGroupId(id);
        permGroupMapper.deleteById(id);
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
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        menuMapper.insert(entity);
        return entity;
    }

    @Transactional
    public SysMenuEntity updateMenu(Long id, SysMenuSaveDto dto) {
        SysMenuEntity entity = requireMenu(id);
        entity.setParentId(parseId(dto.getParentId()));
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
                    .setCreatedAt(now);
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
                    .setCreatedAt(now);
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

    private SysMenuEntity fromMenuDto(SysMenuSaveDto dto) {
        return new SysMenuEntity()
                .setParentId(parseId(dto.getParentId()))
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
