package cn.miyf.service;

import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.bean.entity.SysPermGroupEntity;
import cn.miyf.bean.entity.SysPermGroupItemEntity;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysRolePermGroupEntity;
import cn.miyf.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.repository.mapper.SysPermGroupMapper;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysRolePermGroupMapper;
import cn.miyf.security.MiyfPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 启动时扫描 Controller 上的 {@link MiyfPermission}，upsert 权限/权限组，并为 SUPER_ADMIN 绑定全部权限组。
 * <p>
 * 孤儿权限保留（不自动物理删除）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Component
public class PermissionBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionBootstrap.class);
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final ApplicationContext applicationContext;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;

    /**
     * 构造权限启动引导。
     *
     * @param applicationContext   Spring 上下文
     * @param snowflakeIdGenerator 雪花 ID
     * @param permissionMapper     权限 Mapper
     * @param permGroupMapper      权限组 Mapper
     * @param permGroupItemMapper  权限组条目 Mapper
     * @param roleMapper           角色 Mapper
     * @param rolePermGroupMapper  角色权限组 Mapper
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public PermissionBootstrap(ApplicationContext applicationContext,
                               SnowflakeIdGenerator snowflakeIdGenerator,
                               SysPermissionMapper permissionMapper,
                               SysPermGroupMapper permGroupMapper,
                               SysPermGroupItemMapper permGroupItemMapper,
                               SysRoleMapper roleMapper,
                               SysRolePermGroupMapper rolePermGroupMapper) {
        this.applicationContext = applicationContext;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.permissionMapper = permissionMapper;
        this.permGroupMapper = permGroupMapper;
        this.permGroupItemMapper = permGroupItemMapper;
        this.roleMapper = roleMapper;
        this.rolePermGroupMapper = rolePermGroupMapper;
    }

    /**
     * 扫描并同步权限元数据。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, MiyfPermission> scanned = scanMiyfPermissions();
        log.info("MiyfPermission scan count={}", scanned.size());
        Map<String, Long> groupIdByCode = new HashMap<>();
        Instant now = Instant.now();
        for (MiyfPermission meta : scanned.values()) {
            Long groupId = upsertGroup(meta, groupIdByCode, now);
            Long permissionId = upsertPermission(meta, now);
            ensureGroupItem(groupId, permissionId, now);
        }
        bindAllGroupsToSuperAdmin(groupIdByCode.values(), now);
    }

    /**
     * 扫描全部 Controller Bean 方法上的 {@link MiyfPermission}。
     *
     * @return code → 注解（后者覆盖前者）
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private Map<String, MiyfPermission> scanMiyfPermissions() {
        Map<String, MiyfPermission> result = new HashMap<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
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
            for (Method method : userClass.getDeclaredMethods()) {
                MiyfPermission[] annotations = method.getAnnotationsByType(MiyfPermission.class);
                if (annotations.length == 0) {
                    continue;
                }
                for (MiyfPermission annotation : annotations) {
                    if (!StringUtils.hasText(annotation.code())) {
                        continue;
                    }
                    result.put(annotation.code().trim(), annotation);
                }
            }
        }
        return result;
    }

    private boolean isController(Class<?> type) {
        return AnnotationUtils.findAnnotation(type, RestController.class) != null
                || AnnotationUtils.findAnnotation(type, Controller.class) != null;
    }

    /**
     * upsert 权限组。
     *
     * @param meta          注解
     * @param groupIdByCode 缓存
     * @param now           当前时间
     * @return 权限组 ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private Long upsertGroup(MiyfPermission meta, Map<String, Long> groupIdByCode, Instant now) {
        String groupCode = StringUtils.hasText(meta.groupCode()) ? meta.groupCode().trim() : "default";
        if (groupIdByCode.containsKey(groupCode)) {
            return groupIdByCode.get(groupCode);
        }
        SysPermGroupEntity existing = permGroupMapper.selectByCode(groupCode);
        String groupName = StringUtils.hasText(meta.groupName()) ? meta.groupName().trim() : groupCode;
        if (existing == null) {
            SysPermGroupEntity created = new SysPermGroupEntity()
                    .setCode(groupCode)
                    .setName(groupName)
                    .setSortOrder(0);
            created.setId(snowflakeIdGenerator.nextId());
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            permGroupMapper.insert(created);
            groupIdByCode.put(groupCode, created.getId());
            return created.getId();
        }
        existing.setName(groupName);
        existing.setUpdatedAt(now);
        permGroupMapper.updateById(existing);
        groupIdByCode.put(groupCode, existing.getId());
        return existing.getId();
    }

    /**
     * upsert 权限（孤儿权限不自动删除）。
     *
     * @param meta 注解
     * @param now  当前时间
     * @return 权限 ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private Long upsertPermission(MiyfPermission meta, Instant now) {
        String code = meta.code().trim();
        String name = StringUtils.hasText(meta.name()) ? meta.name().trim() : code;
        String groupCode = StringUtils.hasText(meta.groupCode()) ? meta.groupCode().trim() : "default";
        SysPermissionEntity existing = permissionMapper.selectByCode(code);
        if (existing == null) {
            SysPermissionEntity created = new SysPermissionEntity()
                    .setCode(code)
                    .setName(name)
                    .setGroupCode(groupCode);
            created.setId(snowflakeIdGenerator.nextId());
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            permissionMapper.insert(created);
            return created.getId();
        }
        existing.setName(name);
        existing.setGroupCode(groupCode);
        existing.setUpdatedAt(now);
        permissionMapper.updateById(existing);
        return existing.getId();
    }

    /**
     * 确保权限组包含该权限。
     *
     * @param groupId      权限组 ID
     * @param permissionId 权限 ID
     * @param now          当前时间
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private void ensureGroupItem(Long groupId, Long permissionId, Instant now) {
        SysPermGroupItemEntity existing = permGroupItemMapper.selectByGroupAndPermission(groupId, permissionId);
        if (existing != null) {
            return;
        }
        SysPermGroupItemEntity item = new SysPermGroupItemEntity()
                .setId(snowflakeIdGenerator.nextId())
                .setGroupId(groupId)
                .setPermissionId(permissionId)
                .setCreatedAt(now);
        permGroupItemMapper.insert(item);
    }

    /**
     * 将全部权限组绑定到 SUPER_ADMIN（已绑定则跳过）。
     *
     * @param groupIds 本次扫描涉及的权限组 ID
     * @param now      当前时间
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private void bindAllGroupsToSuperAdmin(Iterable<Long> groupIds, Instant now) {
        SysRoleEntity superAdmin = roleMapper.selectByCode(SUPER_ADMIN);
        if (superAdmin == null) {
            log.warn("SUPER_ADMIN role not found, skip perm-group binding");
            return;
        }
        // 绑定库中全部权限组，确保种子菜单权限也能覆盖
        Set<Long> allGroupIds = new HashSet<>();
        for (Long id : groupIds) {
            allGroupIds.add(id);
        }
        permGroupMapper.selectList(null).forEach(g -> allGroupIds.add(g.getId()));
        for (Long groupId : allGroupIds) {
            if (rolePermGroupMapper.selectByRoleAndGroup(superAdmin.getId(), groupId) != null) {
                continue;
            }
            SysRolePermGroupEntity bind = new SysRolePermGroupEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setRoleId(superAdmin.getId())
                    .setGroupId(groupId)
                    .setCreatedAt(now);
            rolePermGroupMapper.insert(bind);
        }
        log.info("SUPER_ADMIN bound perm-group count={}", allGroupIds.size());
    }
}
