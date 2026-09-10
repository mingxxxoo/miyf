package cn.miyf.permission.service;

import cn.miyf.auth.bean.entity.SysUserEntity;
import cn.miyf.auth.bean.entity.SysUserRoleEntity;
import cn.miyf.auth.repository.mapper.SysUserMapper;
import cn.miyf.auth.repository.mapper.SysUserRoleMapper;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.PlatformRoles;
import cn.miyf.auth.security.PopedomGroup;
import cn.miyf.auth.security.PopedomRole;
import cn.miyf.auth.security.PopedomRoleType;
import cn.miyf.auth.security.PopedomRoles;
import cn.miyf.auth.security.PopedomScope;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.permission.bean.entity.SysPermGroupEntity;
import cn.miyf.permission.bean.entity.SysPermGroupItemEntity;
import cn.miyf.permission.bean.entity.SysPermissionEntity;
import cn.miyf.permission.bean.entity.SysRoleEntity;
import cn.miyf.permission.bean.entity.SysRolePermGroupEntity;
import cn.miyf.permission.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.permission.repository.mapper.SysPermGroupMapper;
import cn.miyf.permission.repository.mapper.SysPermissionMapper;
import cn.miyf.permission.repository.mapper.SysRoleMapper;
import cn.miyf.permission.repository.mapper.SysRolePermGroupMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 权限启动引导：严格按「清空 → 扫描 → 重建」落地。
 * <ol>
 *   <li>清空权限组、权限、角色及关联（含用户角色绑定）</li>
 *   <li>扫描 {@link PopedomRole}、{@link PopedomGroup}、{@link MiyfPermission}</li>
 *   <li>按顺序重建角色、权限组、权限树（个人|单位|超管 → 接口业务 → 接口名称）并绑定角色</li>
 * </ol>
 * 超管角色绑定全部权限组；种子用户 {@code admin} 重新绑定超管角色。
 *
 * @author XieMingJie
 * @since 2026-09-05
 * @history 1.00 2026-09-05 XieMingJie Created.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionBootstrap implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final long SUPER_ADMIN_ROLE_ID = 20001L;
    private static final long ADMIN_USER_ROLE_ID = 40001L;

    private final ApplicationContext applicationContext;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 清空后扫描注解并重建权限与角色数据。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();

        // 1) 清空
        wipePermissionData();

        // 2) 扫描
        List<PopedomRole> roleDefs = scanRoleDefinitions();
        List<ApiSpec> apiSpecs = scanApiSpecs();
        Map<String, PopedomGroup> groupsByCode = collectGroups(apiSpecs);

        // 3) 重建：角色 → 权限组 → 权限 → 绑定
        Map<String, Long> roleIdByCode = rebuildRoles(roleDefs, now);
        Map<String, Long> groupIdByCode = rebuildGroups(groupsByCode, now);
        rebuildPermissions(apiSpecs, groupIdByCode, now);
        bindRolesToGroups(groupsByCode, roleIdByCode, groupIdByCode, now);
        bindAllGroupsToSuperAdmin(roleIdByCode, groupIdByCode.values(), now);
        rebindSeedAdmin(roleIdByCode, now);

        log.info("Permission rebuild done: roles={}, groups={}, apis={}",
                roleIdByCode.size(), groupIdByCode.size(), apiSpecs.size());
    }

    /**
     * 物理清空权限相关表（顺序兼顾外键）。
     */
    private void wipePermissionData() {
        userRoleMapper.delete(Wrappers.lambdaQuery());
        rolePermGroupMapper.delete(Wrappers.lambdaQuery());
        permGroupItemMapper.delete(Wrappers.lambdaQuery());
        permissionMapper.delete(Wrappers.lambdaQuery());
        permGroupMapper.delete(Wrappers.lambdaQuery());
        roleMapper.delete(Wrappers.lambdaQuery());
    }

    private List<PopedomRole> scanRoleDefinitions() {
        Map<String, PopedomRole> byCode = new LinkedHashMap<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                continue;
            }
            Class<?> type = ClassUtils.getUserClass(AopUtils.getTargetClass(bean));
            PopedomRoles multi = AnnotationUtils.findAnnotation(type, PopedomRoles.class);
            if (multi != null) {
                for (PopedomRole role : multi.value()) {
                    putRoleDef(byCode, role);
                }
            }
            PopedomRole single = AnnotationUtils.findAnnotation(type, PopedomRole.class);
            if (single != null) {
                putRoleDef(byCode, single);
            }
        }
        return new ArrayList<>(byCode.values());
    }

    private static void putRoleDef(Map<String, PopedomRole> byCode, PopedomRole role) {
        if (role == null || !StringUtils.hasText(role.code())) {
            return;
        }
        byCode.putIfAbsent(role.code().trim(), role);
    }

    private List<ApiSpec> scanApiSpecs() {
        List<ApiSpec> specs = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(AopUtils.getTargetClass(bean));
            if (!isController(userClass)) {
                continue;
            }
            PopedomGroup popedom = AnnotationUtils.findAnnotation(userClass, PopedomGroup.class);
            if (popedom == null) {
                continue;
            }
            Tag tag = AnnotationUtils.findAnnotation(userClass, Tag.class);
            String bizLabel = resolveBizLabel(userClass, tag);
            for (Method method : userClass.getDeclaredMethods()) {
                Set<MiyfPermission> permissionSet =
                        AnnotatedElementUtils.getMergedRepeatableAnnotations(method, MiyfPermission.class);
                for (MiyfPermission meta : permissionSet) {
                    if (meta == null || !StringUtils.hasText(meta.code())) {
                        continue;
                    }
                    String code = meta.code().trim();
                    if (!seenCodes.add(code)) {
                        continue;
                    }
                    String apiName = resolveApiName(meta, method);
                    specs.add(new ApiSpec(popedom, meta, bizLabel, apiName));
                }
            }
        }
        return specs;
    }

    private static Map<String, PopedomGroup> collectGroups(List<ApiSpec> apiSpecs) {
        Map<String, PopedomGroup> map = new LinkedHashMap<>();
        for (ApiSpec spec : apiSpecs) {
            PopedomGroup group = spec.popedom();
            map.putIfAbsent(group.value().trim(), group);
        }
        return map;
    }

    private Map<String, Long> rebuildRoles(List<PopedomRole> roleDefs, Instant now) {
        Map<String, Long> roleIdByCode = new HashMap<>();
        for (PopedomRole def : roleDefs) {
            String code = def.code().trim();
            long id = PlatformRoles.SUPER_ADMIN.equals(code) ? SUPER_ADMIN_ROLE_ID : snowflakeIdGenerator.nextId();
            SysRoleEntity role = new SysRoleEntity()
                    .setCode(code)
                    .setName(def.name().trim())
                    .setDescription(StringUtils.hasText(def.description()) ? def.description().trim() : null)
                    .setIsDefault(def.type() == PopedomRoleType.DEFAULT)
                    .setProduct(StringUtils.hasText(def.product()) ? def.product().trim() : null)
                    .setDataScope(def.type() == PopedomRoleType.SUPER ? "ALL" : "ORG");
            role.setId(id);
            role.setCreateTime(now);
            role.setLastModifyTime(now);
            roleMapper.insert(role);
            roleIdByCode.put(code, id);
        }
        // 确保超管角色一定存在（即使未扫到定义）
        if (!roleIdByCode.containsKey(PlatformRoles.SUPER_ADMIN)) {
            SysRoleEntity superAdmin = new SysRoleEntity()
                    .setCode(PlatformRoles.SUPER_ADMIN)
                    .setName("超级管理员")
                    .setDescription("全局超管：绑定全部权限组")
                    .setIsDefault(false)
                    .setDataScope("ALL");
            superAdmin.setId(SUPER_ADMIN_ROLE_ID);
            superAdmin.setCreateTime(now);
            superAdmin.setLastModifyTime(now);
            roleMapper.insert(superAdmin);
            roleIdByCode.put(PlatformRoles.SUPER_ADMIN, SUPER_ADMIN_ROLE_ID);
        }
        return roleIdByCode;
    }

    private Map<String, Long> rebuildGroups(Map<String, PopedomGroup> groupsByCode, Instant now) {
        Map<String, Long> groupIdByCode = new HashMap<>();
        for (PopedomGroup group : groupsByCode.values()) {
            String code = group.value().trim();
            long id = Long.parseLong(code);
            SysPermGroupEntity entity = new SysPermGroupEntity()
                    .setCode(code)
                    .setName(resolveGroupDisplayName(group))
                    .setProduct(StringUtils.hasText(group.product()) ? group.product().trim() : null)
                    .setScope(group.scope().name())
                    .setSortOrder(group.sort());
            entity.setId(id);
            entity.setCreateTime(now);
            entity.setLastModifyTime(now);
            permGroupMapper.insert(entity);
            groupIdByCode.put(code, id);
        }
        return groupIdByCode;
    }

    private void rebuildPermissions(List<ApiSpec> apiSpecs,
                                    Map<String, Long> groupIdByCode,
                                    Instant now) {
        Map<String, AtomicInteger> bizSeqByGroup = new HashMap<>();
        Map<String, AtomicInteger> apiSeqByGroup = new HashMap<>();
        Map<String, Long> scopeRootIdByGroup = new HashMap<>();
        Map<String, Long> bizNodeIdByKey = new HashMap<>();

        for (ApiSpec spec : apiSpecs) {
            PopedomGroup group = spec.popedom();
            String groupCode = group.value().trim();
            Long groupId = groupIdByCode.get(groupCode);
            if (groupId == null) {
                continue;
            }
            PopedomScope scope = group.scope();
            String scopeLabel = scope.getLabel();

            Long rootId = scopeRootIdByGroup.computeIfAbsent(groupCode, gc ->
                    insertTreeNode(gc, 0, "ROOT",
                            treeCode(gc, "scope"),
                            scopeLabel,
                            scopeLabel,
                            null,
                            group.product(),
                            0,
                            now));

            String bizKey = groupCode + "|" + spec.bizLabel();
            Long bizId = bizNodeIdByKey.get(bizKey);
            if (bizId == null) {
                int bizSeq = bizSeqByGroup.computeIfAbsent(groupCode, k -> new AtomicInteger(100)).getAndIncrement();
                String bizTreeName = scopeLabel + "-" + spec.bizLabel();
                bizId = insertTreeNode(groupCode, bizSeq, "BIZ",
                        treeCode(groupCode, "biz:" + spec.bizLabel()),
                        spec.bizLabel(),
                        bizTreeName,
                        rootId,
                        group.product(),
                        bizSeq,
                        now);
                bizNodeIdByKey.put(bizKey, bizId);
            }

            int apiSeq = apiSeqByGroup.computeIfAbsent(groupCode, k -> new AtomicInteger(10000)).getAndIncrement();
            String apiTreeName = scopeLabel + "-" + spec.bizLabel() + "-" + spec.apiName();
            long apiId = insertApiLeaf(groupCode, apiSeq, spec.meta().code().trim(), spec.apiName(),
                    apiTreeName, bizId, group.product(), now);

            SysPermGroupItemEntity item = new SysPermGroupItemEntity()
                    .setGroupId(groupId)
                    .setPermissionId(apiId);
            item.setId(snowflakeIdGenerator.nextId());
            item.setCreateTime(now);
            permGroupItemMapper.insert(item);
        }
    }

    private long insertTreeNode(String groupCode,
                                int seq,
                                String nodeType,
                                String code,
                                String name,
                                String treeName,
                                Long parentId,
                                String product,
                                int sortOrder,
                                Instant now) {
        String permNo = formatPermNo(groupCode, seq);
        long id = parsePermId(permNo);
        SysPermissionEntity node = new SysPermissionEntity()
                .setCode(code)
                .setName(name)
                .setGroupCode(groupCode)
                .setPermNo(permNo)
                .setParentId(parentId)
                .setProduct(product)
                .setTreeName(treeName)
                .setNodeType(nodeType)
                .setSortOrder(sortOrder);
        node.setId(id);
        node.setCreateTime(now);
        node.setLastModifyTime(now);
        permissionMapper.insert(node);
        return id;
    }

    private long insertApiLeaf(String groupCode,
                               int seq,
                               String code,
                               String name,
                               String treeName,
                               Long parentId,
                               String product,
                               Instant now) {
        String permNo = formatPermNo(groupCode, seq);
        long id = parsePermId(permNo);
        SysPermissionEntity api = new SysPermissionEntity()
                .setCode(code)
                .setName(name)
                .setGroupCode(groupCode)
                .setPermNo(permNo)
                .setParentId(parentId)
                .setProduct(product)
                .setTreeName(treeName)
                .setNodeType("API")
                .setSortOrder(seq);
        api.setId(id);
        api.setCreateTime(now);
        api.setLastModifyTime(now);
        permissionMapper.insert(api);
        return id;
    }

    private void bindRolesToGroups(Map<String, PopedomGroup> groupsByCode,
                                   Map<String, Long> roleIdByCode,
                                   Map<String, Long> groupIdByCode,
                                   Instant now) {
        for (Map.Entry<String, PopedomGroup> entry : groupsByCode.entrySet()) {
            Long groupId = groupIdByCode.get(entry.getKey());
            if (groupId == null) {
                continue;
            }
            for (String roleCode : entry.getValue().roles()) {
                if (!StringUtils.hasText(roleCode)) {
                    continue;
                }
                Long roleId = roleIdByCode.get(roleCode.trim());
                if (roleId == null) {
                    log.warn("Role code {} declared on group {} not found in role definitions", roleCode, entry.getKey());
                    continue;
                }
                insertRoleGroupBind(roleId, groupId, now);
            }
        }
    }

    /**
     * 超管角色绑定当前全部权限组（配置类型 SUPER，而非名称字符串）。
     */
    private void bindAllGroupsToSuperAdmin(Map<String, Long> roleIdByCode,
                                           Iterable<Long> groupIds,
                                           Instant now) {
        Long superRoleId = roleIdByCode.get(PlatformRoles.SUPER_ADMIN);
        if (superRoleId == null) {
            log.warn("SUPER_ADMIN role missing after rebuild");
            return;
        }
        for (Long groupId : groupIds) {
            insertRoleGroupBind(superRoleId, groupId, now);
        }
    }

    private void insertRoleGroupBind(Long roleId, Long groupId, Instant now) {
        if (rolePermGroupMapper.selectByRoleAndGroup(roleId, groupId) != null) {
            return;
        }
        SysRolePermGroupEntity bind = new SysRolePermGroupEntity()
                .setRoleId(roleId)
                .setGroupId(groupId);
        bind.setId(snowflakeIdGenerator.nextId());
        bind.setCreateTime(now);
        rolePermGroupMapper.insert(bind);
    }

    private void rebindSeedAdmin(Map<String, Long> roleIdByCode, Instant now) {
        SysUserEntity admin = userMapper.selectByUsername(ADMIN_USERNAME);
        Long superRoleId = roleIdByCode.get(PlatformRoles.SUPER_ADMIN);
        if (admin == null || superRoleId == null) {
            log.warn("Skip admin rebind: adminUser={}, superRoleId={}", admin != null, superRoleId);
            return;
        }
        SysUserRoleEntity bind = new SysUserRoleEntity()
                .setUserId(admin.getId())
                .setRoleId(superRoleId);
        bind.setId(ADMIN_USER_ROLE_ID);
        bind.setCreateTime(now);
        userRoleMapper.insert(bind);
    }

    private static String resolveGroupDisplayName(PopedomGroup group) {
        if (StringUtils.hasText(group.name())) {
            return group.name().trim();
        }
        return group.scope().getLabel() + "-" + group.service().trim();
    }

    private static String resolveBizLabel(Class<?> userClass, Tag tag) {
        if (tag != null && StringUtils.hasText(tag.name())) {
            return tag.name().trim();
        }
        String simple = userClass.getSimpleName();
        return simple.replace("Controller", "").replace("Admin", "");
    }

    private static String resolveApiName(MiyfPermission meta, Method method) {
        if (StringUtils.hasText(meta.name())) {
            return meta.name().trim();
        }
        Operation operation = AnnotationUtils.findAnnotation(method, Operation.class);
        if (operation != null && StringUtils.hasText(operation.summary())) {
            return operation.summary().trim();
        }
        return meta.code().trim();
    }

    private static boolean isController(Class<?> type) {
        return AnnotationUtils.findAnnotation(type, RestController.class) != null
                || AnnotationUtils.findAnnotation(type, Controller.class) != null;
    }

    private static String treeCode(String groupCode, String suffix) {
        return "tree:" + groupCode + ":" + suffix;
    }

    private static String formatPermNo(String groupCode, int seq) {
        return groupCode + String.format("%08d", seq);
    }

    private static long parsePermId(String permNo) {
        return Long.parseLong(permNo);
    }

    private record ApiSpec(PopedomGroup popedom, MiyfPermission meta, String bizLabel, String apiName) {
    }
}
