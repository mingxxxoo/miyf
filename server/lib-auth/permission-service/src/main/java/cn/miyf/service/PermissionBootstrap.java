package cn.miyf.service;

import cn.miyf.bean.entity.SysPermGroupEntity;
import cn.miyf.bean.entity.SysPermGroupItemEntity;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysRolePermGroupEntity;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysPermGroupItemMapper;
import cn.miyf.repository.mapper.SysPermGroupMapper;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysRolePermGroupMapper;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动扫描 {@link PopedomGroup} + {@link MiyfPermission}，写入权限树与权限组。
 * <p>
 * 16 位编号 = 8 位组编码 + 8 位序号；根节点序号 {@code 00000000}。
 * 树层级：域(ROOT) → 产品(PRODUCT) → 业务(BIZ) → 接口(API)。
 * JWT 仅下发 API 节点权限码。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Component
public class PermissionBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionBootstrap.class);
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String FALLBACK_GROUP = "19990000";
    private static final String FALLBACK_GROUP_NAME = "未分组";

    private final ApplicationContext applicationContext;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;

    /**
     * 各组下一个叶子序号（从 10000 起）
     */
    private final Map<String, AtomicInteger> leafSeqByGroup = new ConcurrentHashMap<>();
    /**
     * 各组产品序号（1～99）
     */
    private final Map<String, Map<String, Integer>> productSeqByGroup = new ConcurrentHashMap<>();
    /**
     * 各组业务序号（100～9999，步进预留）
     */
    private final Map<String, Map<String, Integer>> bizSeqByGroup = new ConcurrentHashMap<>();
    private final AtomicInteger productCounter = new AtomicInteger(0);
    private final AtomicInteger bizCounter = new AtomicInteger(0);

    /**
     * 构造权限启动引导。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     * @history 1.01 2026-09-06 XieMingJie 支持 PopedomGroup 与 16 位权限树。
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
     * @history 1.01 2026-09-06 XieMingJie 树形同步。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        Map<String, Long> groupIdByCode = new HashMap<>();
        int apiCount = 0;
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
            PopedomGroup popedom = AnnotationUtils.findAnnotation(userClass, PopedomGroup.class);
            Tag tag = AnnotationUtils.findAnnotation(userClass, Tag.class);
            String bizLabel = resolveBizLabel(userClass, tag);

            for (Method method : userClass.getDeclaredMethods()) {
                MiyfPermission[] annotations = method.getAnnotationsByType(MiyfPermission.class);
                if (annotations.length == 0) {
                    continue;
                }
                for (MiyfPermission meta : annotations) {
                    if (!StringUtils.hasText(meta.code())) {
                        continue;
                    }
                    syncApiPermission(popedom, meta, bizLabel, groupIdByCode, now);
                    apiCount++;
                }
            }
        }
        bindAllGroupsToSuperAdmin(groupIdByCode.values(), now);
        log.info("Permission tree sync done, apiCount={}, groups={}", apiCount, groupIdByCode.size());
    }

    private void syncApiPermission(PopedomGroup popedom,
                                   MiyfPermission meta,
                                   String bizLabel,
                                   Map<String, Long> groupIdByCode,
                                   Instant now) {
        String groupCode = resolveGroupCode(popedom, meta);
        String groupName = resolveGroupName(popedom, meta, groupCode);
        String product = resolveProduct(popedom);
        Long groupId = upsertGroup(groupCode, groupName, product, popedom == null ? 0 : popedom.sort(),
                groupIdByCode, now);

        SysPermissionEntity root = ensureTreeNode(
                groupCode, 0, "ROOT",
                treeCode(groupCode, "root"),
                groupName,
                groupName,
                null,
                product,
                0,
                now);

        SysPermissionEntity productNode = ensureTreeNode(
                groupCode,
                productSeq(groupCode, product),
                "PRODUCT",
                treeCode(groupCode, "product:" + product),
                productDisplay(product),
                groupName + "-" + productDisplay(product),
                root.getId(),
                product,
                productSeq(groupCode, product),
                now);

        int bizSeq = bizSeq(groupCode, bizLabel);
        SysPermissionEntity bizNode = ensureTreeNode(
                groupCode,
                bizSeq,
                "BIZ",
                treeCode(groupCode, "biz:" + bizLabel),
                bizLabel,
                groupName + "-" + productDisplay(product) + "-" + bizLabel,
                productNode.getId(),
                product,
                bizSeq,
                now);

        String apiName = StringUtils.hasText(meta.name()) ? meta.name().trim() : meta.code().trim();
        String treeName = groupName + "-" + productDisplay(product) + "-" + bizLabel + "-" + apiName;
        int leafSeq = nextLeafSeq(groupCode);
        SysPermissionEntity api = upsertApiLeaf(
                meta.code().trim(),
                apiName,
                groupCode,
                product,
                treeName,
                bizNode.getId(),
                leafSeq,
                now);
        ensureGroupItem(groupId, api.getId(), now);
    }

    private SysPermissionEntity ensureTreeNode(String groupCode,
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
        SysPermissionEntity byCode = permissionMapper.selectByCode(code);
        SysPermissionEntity byNo = permissionMapper.selectByPermNo(permNo);
        SysPermissionEntity existing = byCode != null ? byCode : byNo;
        if (existing == null) {
            SysPermissionEntity created = new SysPermissionEntity()
                    .setCode(code)
                    .setName(name)
                    .setGroupCode(groupCode)
                    .setPermNo(permNo)
                    .setParentId(parentId)
                    .setProduct(product)
                    .setTreeName(treeName)
                    .setNodeType(nodeType)
                    .setSortOrder(sortOrder);
            created.setId(parsePermId(permNo));
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            permissionMapper.insert(created);
            return created;
        }
        existing.setName(name);
        existing.setGroupCode(groupCode);
        existing.setPermNo(permNo);
        existing.setParentId(parentId);
        existing.setProduct(product);
        existing.setTreeName(treeName);
        existing.setNodeType(nodeType);
        existing.setSortOrder(sortOrder);
        existing.setUpdatedAt(now);
        permissionMapper.updateById(existing);
        return existing;
    }

    private SysPermissionEntity upsertApiLeaf(String code,
                                              String name,
                                              String groupCode,
                                              String product,
                                              String treeName,
                                              Long parentId,
                                              int leafSeq,
                                              Instant now) {
        String permNo = formatPermNo(groupCode, leafSeq);
        SysPermissionEntity existing = permissionMapper.selectByCode(code);
        if (existing == null) {
            SysPermissionEntity created = new SysPermissionEntity()
                    .setCode(code)
                    .setName(name)
                    .setGroupCode(groupCode)
                    .setPermNo(permNo)
                    .setParentId(parentId)
                    .setProduct(product)
                    .setTreeName(treeName)
                    .setNodeType("API")
                    .setSortOrder(leafSeq);
            // 已有雪花体系兼容：新 API 用 16 位作主键
            created.setId(parsePermId(permNo));
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            permissionMapper.insert(created);
            return created;
        }
        existing.setName(name);
        existing.setGroupCode(groupCode);
        if (!StringUtils.hasText(existing.getPermNo())) {
            existing.setPermNo(permNo);
        }
        existing.setParentId(parentId);
        existing.setProduct(product);
        existing.setTreeName(treeName);
        existing.setNodeType("API");
        existing.setSortOrder(leafSeq);
        existing.setUpdatedAt(now);
        permissionMapper.updateById(existing);
        return existing;
    }

    private Long upsertGroup(String groupCode,
                             String groupName,
                             String product,
                             int sort,
                             Map<String, Long> groupIdByCode,
                             Instant now) {
        if (groupIdByCode.containsKey(groupCode)) {
            return groupIdByCode.get(groupCode);
        }
        SysPermGroupEntity existing = permGroupMapper.selectByCode(groupCode);
        if (existing == null) {
            SysPermGroupEntity created = new SysPermGroupEntity()
                    .setCode(groupCode)
                    .setName(groupName)
                    .setProduct(product)
                    .setSortOrder(sort);
            // 8 位组编码可作为稳定主键（小于雪花，但唯一）
            long id;
            try {
                id = Long.parseLong(groupCode);
            } catch (NumberFormatException ex) {
                id = snowflakeIdGenerator.nextId();
            }
            created.setId(id);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            permGroupMapper.insert(created);
            groupIdByCode.put(groupCode, created.getId());
            return created.getId();
        }
        existing.setName(groupName);
        existing.setProduct(product);
        existing.setSortOrder(sort);
        existing.setUpdatedAt(now);
        permGroupMapper.updateById(existing);
        groupIdByCode.put(groupCode, existing.getId());
        return existing.getId();
    }

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

    private void bindAllGroupsToSuperAdmin(Iterable<Long> groupIds, Instant now) {
        SysRoleEntity superAdmin = roleMapper.selectByCode(SUPER_ADMIN);
        if (superAdmin == null) {
            log.warn("SUPER_ADMIN role not found, skip perm-group binding");
            return;
        }
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

    private boolean isController(Class<?> type) {
        return AnnotationUtils.findAnnotation(type, RestController.class) != null
                || AnnotationUtils.findAnnotation(type, Controller.class) != null;
    }

    private static String resolveGroupCode(PopedomGroup popedom, MiyfPermission meta) {
        if (popedom != null && StringUtils.hasText(popedom.value())) {
            return normalizeGroupCode(popedom.value().trim());
        }
        if (StringUtils.hasText(meta.groupCode()) && meta.groupCode().trim().matches("\\d{8}")) {
            return meta.groupCode().trim();
        }
        return FALLBACK_GROUP;
    }

    private static String resolveGroupName(PopedomGroup popedom, MiyfPermission meta, String groupCode) {
        if (popedom != null && StringUtils.hasText(popedom.name())) {
            return popedom.name().trim();
        }
        if (StringUtils.hasText(meta.groupName())) {
            return meta.groupName().trim();
        }
        if (FALLBACK_GROUP.equals(groupCode)) {
            return FALLBACK_GROUP_NAME;
        }
        return groupCode;
    }

    private static String resolveProduct(PopedomGroup popedom) {
        if (popedom != null && StringUtils.hasText(popedom.product())) {
            return popedom.product().trim();
        }
        return "platform";
    }

    private static String resolveBizLabel(Class<?> userClass, Tag tag) {
        if (tag != null && StringUtils.hasText(tag.name())) {
            return tag.name().trim();
        }
        String simple = userClass.getSimpleName();
        if (simple.endsWith("Controller")) {
            simple = simple.substring(0, simple.length() - "Controller".length());
        }
        if (simple.startsWith("Admin")) {
            simple = simple.substring("Admin".length());
        }
        return simple.isEmpty() ? userClass.getSimpleName() : simple;
    }

    private static String normalizeGroupCode(String raw) {
        if (raw.matches("\\d{8}")) {
            return raw;
        }
        // 非 8 位数字时落入未分组，避免破坏编号规则
        return FALLBACK_GROUP;
    }

    private static String formatPermNo(String groupCode, int seq) {
        return groupCode + String.format("%08d", seq);
    }

    private static long parsePermId(String permNo) {
        return Long.parseLong(permNo);
    }

    private static String treeCode(String groupCode, String suffix) {
        return "tree:" + groupCode + ":" + suffix;
    }

    private static String productDisplay(String product) {
        return switch (product) {
            case "kitchen" -> "厨房";
            case "health" -> "健康";
            case "iam" -> "权限";
            case "platform" -> "平台";
            default -> product;
        };
    }

    private int productSeq(String groupCode, String product) {
        Map<String, Integer> map = productSeqByGroup.computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(product, k -> {
            int n = productCounter.incrementAndGet();
            // 产品节点占用 1～99
            return Math.min(n, 99);
        });
    }

    private int bizSeq(String groupCode, String bizLabel) {
        Map<String, Integer> map = bizSeqByGroup.computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(bizLabel, k -> {
            int n = bizCounter.incrementAndGet();
            // 业务节点 100～9999
            return 100 + Math.min(n, 9899);
        });
    }

    private int nextLeafSeq(String groupCode) {
        AtomicInteger counter = leafSeqByGroup.computeIfAbsent(groupCode, k -> new AtomicInteger(9999));
        return counter.incrementAndGet();
    }
}
