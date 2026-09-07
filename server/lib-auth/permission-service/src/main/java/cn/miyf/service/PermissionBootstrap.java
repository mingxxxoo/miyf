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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动扫描 {@link PopedomGroup} + {@link MiyfPermission}，写入权限树与权限组。
 * <p>
 * 16 位编号 = 8 位组编码 + 8 位序号；根节点序号 {@code 00000000}。
 * 树层级：域(ROOT) → 产品(PRODUCT) → 业务(BIZ) → 接口(API)。
 * JWT 仅下发 API 节点权限码。
 * <p>
 * 每次启动：清理扫描不到 / 不合规的权限及授权关联，再按标准重建。
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
    private static final Set<String> NODE_TYPES = Set.of("ROOT", "PRODUCT", "BIZ", "API");

    private final ApplicationContext applicationContext;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SysPermissionMapper permissionMapper;
    private final SysPermGroupMapper permGroupMapper;
    private final SysPermGroupItemMapper permGroupItemMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermGroupMapper rolePermGroupMapper;

    private final Map<String, AtomicInteger> leafSeqByGroup = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> productSeqByGroup = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> bizSeqByGroup = new ConcurrentHashMap<>();
    private final AtomicInteger productCounter = new AtomicInteger(0);
    private final AtomicInteger bizCounter = new AtomicInteger(0);
    /** 启动同步前已有条目的权限组：不再 ensure 回填缺失 API（尊重人工裁剪）。 */
    private final Set<Long> groupsLockedFromRefill = ConcurrentHashMap.newKeySet();

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
     * 扫描并同步权限元数据：启动时先清理孤儿/不合规权限，再按标准 upsert 重建。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        List<ApiSpec> specs = scanApiSpecs();
        Set<String> expectedApiCodes = new HashSet<>();
        Map<String, String> expectedApiGroup = new HashMap<>();
        Set<String> expectedTreeCodes = new HashSet<>();
        for (ApiSpec spec : specs) {
            String apiCode = spec.meta().code().trim();
            String groupCode = resolveGroupCode(spec.popedom(), spec.meta());
            String product = resolveProduct(spec.popedom());
            expectedApiCodes.add(apiCode);
            expectedApiGroup.put(apiCode, groupCode);
            expectedTreeCodes.add(treeCode(groupCode, "root"));
            expectedTreeCodes.add(treeCode(groupCode, "product:" + product));
            expectedTreeCodes.add(treeCode(groupCode, "biz:" + spec.bizLabel()));
        }

        int purged = purgeStaleAndNonStandard(expectedApiCodes, expectedApiGroup, expectedTreeCodes);
        resetSeqState();
        warmSeqFromDb();
        warmGroupsWithItems();

        Map<String, Long> groupIdByCode = new HashMap<>();
        Set<String> liveTreeCodes = new HashSet<>();
        Set<String> liveGroupCodes = new HashSet<>();
        for (ApiSpec spec : specs) {
            syncApiPermission(spec.popedom(), spec.meta(), spec.bizLabel(), spec.apiName(), groupIdByCode,
                    liveTreeCodes, liveGroupCodes, now);
        }

        int prunedTree = pruneUnusedTreeNodes(liveTreeCodes);
        int prunedGroups = pruneUnusedGroups(liveGroupCodes);
        pruneOrphanGroupItems();
        bindAllGroupsToSuperAdmin(groupIdByCode.values(), now);
        log.info("Permission tree sync done, apiCount={}, groups={}, purged={}, prunedTree={}, prunedGroups={}",
                specs.size(), groupIdByCode.size(), purged, prunedTree, prunedGroups);
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
            Tag tag = AnnotationUtils.findAnnotation(userClass, Tag.class);
            String bizLabel = resolveBizLabel(userClass, tag);
            for (Method method : userClass.getDeclaredMethods()) {
                MiyfPermission[] annotations = method.getAnnotationsByType(MiyfPermission.class);
                for (MiyfPermission meta : annotations) {
                    if (!StringUtils.hasText(meta.code())) {
                        continue;
                    }
                    String code = meta.code().trim();
                    if (!seenCodes.add(code)) {
                        continue;
                    }
                    specs.add(new ApiSpec(popedom, meta, bizLabel, resolveApiName(meta, method)));
                }
            }
        }
        return specs;
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

    /**
     * 删除：扫描不到的 API、不合标准的权限节点（授权条目随 FK / 显式删除清理）。
     */
    private int purgeStaleAndNonStandard(Set<String> expectedApiCodes,
                                         Map<String, String> expectedApiGroup,
                                         Set<String> expectedTreeCodes) {
        List<SysPermissionEntity> all = permissionMapper.selectList(null);
        if (all == null || all.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (SysPermissionEntity p : all) {
            boolean remove = false;
            String nodeType = p.getNodeType();
            if (!StringUtils.hasText(nodeType) || !NODE_TYPES.contains(nodeType)) {
                remove = true;
            } else if ("API".equals(nodeType)) {
                if (!StringUtils.hasText(p.getCode()) || p.getCode().startsWith("tree:")) {
                    remove = true;
                } else if (!expectedApiCodes.contains(p.getCode())) {
                    remove = true;
                } else if (!isStandardApi(p)) {
                    remove = true;
                } else {
                    String expectGroup = expectedApiGroup.get(p.getCode());
                    if (expectGroup != null && !expectGroup.equals(p.getGroupCode())) {
                        remove = true;
                    }
                }
            } else {
                // ROOT / PRODUCT / BIZ
                if (!expectedTreeCodes.contains(p.getCode()) || !isStandardTreeNode(p)) {
                    remove = true;
                }
            }
            if (remove) {
                deletePermissionCascade(p.getId());
                removed++;
            }
        }
        if (removed > 0) {
            log.info("Purged stale/non-standard permissions: {}", removed);
        }
        return removed;
    }

    private void deletePermissionCascade(Long permissionId) {
        if (permissionId == null) {
            return;
        }
        permGroupItemMapper.deleteByPermissionId(permissionId);
        permissionMapper.deleteById(permissionId);
    }

    private int pruneUnusedTreeNodes(Set<String> liveTreeCodes) {
        List<SysPermissionEntity> all = permissionMapper.selectList(Wrappers.<SysPermissionEntity>lambdaQuery()
                .in(SysPermissionEntity::getNodeType, List.of("ROOT", "PRODUCT", "BIZ")));
        int removed = 0;
        for (SysPermissionEntity p : all) {
            if (!liveTreeCodes.contains(p.getCode())) {
                deletePermissionCascade(p.getId());
                removed++;
            }
        }
        return removed;
    }

    private int pruneUnusedGroups(Set<String> liveGroupCodes) {
        List<SysPermGroupEntity> groups = permGroupMapper.selectList(null);
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (SysPermGroupEntity g : groups) {
            if (liveGroupCodes.contains(g.getCode())) {
                continue;
            }
            // 组下若已无 API 条目则可删；仍有条目则保留（防御）
            Long itemCount = permGroupItemMapper.selectCount(Wrappers.<SysPermGroupItemEntity>lambdaQuery()
                    .eq(SysPermGroupItemEntity::getGroupId, g.getId()));
            if (itemCount != null && itemCount > 0) {
                continue;
            }
            rolePermGroupMapper.delete(Wrappers.<SysRolePermGroupEntity>lambdaQuery()
                    .eq(SysRolePermGroupEntity::getGroupId, g.getId()));
            permGroupItemMapper.deleteByGroupId(g.getId());
            permGroupMapper.deleteById(g.getId());
            removed++;
        }
        return removed;
    }

    private void warmGroupsWithItems() {
        groupsLockedFromRefill.clear();
        List<SysPermGroupItemEntity> items = permGroupItemMapper.selectList(null);
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SysPermGroupItemEntity item : items) {
            if (item.getGroupId() != null) {
                groupsLockedFromRefill.add(item.getGroupId());
            }
        }
    }

    private void pruneOrphanGroupItems() {
        List<SysPermGroupItemEntity> items = permGroupItemMapper.selectList(null);
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SysPermGroupItemEntity item : items) {
            if (item.getPermissionId() == null || permissionMapper.selectById(item.getPermissionId()) == null) {
                permGroupItemMapper.deleteById(item.getId());
            }
        }
    }

    private void resetSeqState() {
        leafSeqByGroup.clear();
        productSeqByGroup.clear();
        bizSeqByGroup.clear();
        productCounter.set(0);
        bizCounter.set(0);
    }

    private void warmSeqFromDb() {
        List<SysPermissionEntity> all = permissionMapper.selectList(null);
        if (all == null || all.isEmpty()) {
            return;
        }
        int maxProduct = 0;
        int maxBiz = 0;
        for (SysPermissionEntity p : all) {
            String permNo = p.getPermNo();
            if (!isDigits(permNo, 16)) {
                continue;
            }
            String groupCode = permNo.substring(0, 8);
            int seq = Integer.parseInt(permNo.substring(8));
            String nodeType = p.getNodeType();
            if ("PRODUCT".equals(nodeType) && seq >= 1 && seq <= 99) {
                String product = StringUtils.hasText(p.getProduct()) ? p.getProduct() : "platform";
                productSeqByGroup
                        .computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>())
                        .putIfAbsent(product, seq);
                maxProduct = Math.max(maxProduct, seq);
            } else if ("BIZ".equals(nodeType) && seq >= 100 && seq <= 9999) {
                String bizLabel = bizLabelFromTreeCode(p.getCode());
                if (StringUtils.hasText(bizLabel)) {
                    bizSeqByGroup
                            .computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>())
                            .putIfAbsent(bizLabel, seq);
                }
                maxBiz = Math.max(maxBiz, seq - 100);
            } else if (("API".equals(nodeType) || nodeType == null) && seq >= 10000) {
                AtomicInteger counter = leafSeqByGroup.computeIfAbsent(groupCode, k -> new AtomicInteger(9999));
                counter.updateAndGet(cur -> Math.max(cur, seq));
            }
        }
        if (maxProduct > 0) {
            productCounter.set(maxProduct);
        }
        if (maxBiz > 0) {
            bizCounter.set(maxBiz);
        }
    }

    /** API：16 位编号、主键=编号、组码一致、名称/树名齐全。 */
    private static boolean isStandardApi(SysPermissionEntity p) {
        if (!"API".equals(p.getNodeType())) {
            return false;
        }
        if (!StringUtils.hasText(p.getCode()) || p.getCode().startsWith("tree:")) {
            return false;
        }
        if (!StringUtils.hasText(p.getName()) || !StringUtils.hasText(p.getTreeName())) {
            return false;
        }
        if (!StringUtils.hasText(p.getProduct())) {
            return false;
        }
        String groupCode = p.getGroupCode();
        if (!isDigits(groupCode, 8)) {
            return false;
        }
        String permNo = p.getPermNo();
        if (!isDigits(permNo, 16) || !permNo.startsWith(groupCode)) {
            return false;
        }
        int seq = Integer.parseInt(permNo.substring(8));
        if (seq < 10000) {
            return false;
        }
        try {
            return Objects.equals(p.getId(), Long.parseLong(permNo));
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** 树节点：code/perm_no/id/node_type 符合规划。 */
    private static boolean isStandardTreeNode(SysPermissionEntity p) {
        String nodeType = p.getNodeType();
        String code = p.getCode();
        String groupCode = p.getGroupCode();
        String permNo = p.getPermNo();
        if (!StringUtils.hasText(code) || !code.startsWith("tree:")) {
            return false;
        }
        if (!isDigits(groupCode, 8) || !isDigits(permNo, 16) || !permNo.startsWith(groupCode)) {
            return false;
        }
        if (!StringUtils.hasText(p.getName()) || !StringUtils.hasText(p.getTreeName())) {
            return false;
        }
        int seq = Integer.parseInt(permNo.substring(8));
        try {
            if (!Objects.equals(p.getId(), Long.parseLong(permNo))) {
                return false;
            }
        } catch (NumberFormatException ex) {
            return false;
        }
        return switch (nodeType) {
            case "ROOT" -> seq == 0 && code.equals(treeCode(groupCode, "root"));
            case "PRODUCT" -> seq >= 1 && seq <= 99 && code.contains(":product:");
            case "BIZ" -> seq >= 100 && seq <= 9999 && code.contains(":biz:");
            default -> false;
        };
    }

    private static boolean isDigits(String raw, int len) {
        return StringUtils.hasText(raw) && raw.length() == len && raw.chars().allMatch(Character::isDigit);
    }

    private static String bizLabelFromTreeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        int bizIdx = code.indexOf(":biz:");
        if (bizIdx < 0) {
            return null;
        }
        return code.substring(bizIdx + ":biz:".length());
    }

    private void syncApiPermission(PopedomGroup popedom,
                                   MiyfPermission meta,
                                   String bizLabel,
                                   String apiName,
                                   Map<String, Long> groupIdByCode,
                                   Set<String> liveTreeCodes,
                                   Set<String> liveGroupCodes,
                                   Instant now) {
        String groupCode = resolveGroupCode(popedom, meta);
        String groupName = resolveGroupName(popedom, meta, groupCode);
        String product = resolveProduct(popedom);
        liveGroupCodes.add(groupCode);
        Long groupId = upsertGroup(groupCode, groupName, product, popedom == null ? 0 : popedom.sort(),
                groupIdByCode, now);

        String rootCode = treeCode(groupCode, "root");
        String productCode = treeCode(groupCode, "product:" + product);
        String bizCode = treeCode(groupCode, "biz:" + bizLabel);
        liveTreeCodes.add(rootCode);
        liveTreeCodes.add(productCode);
        liveTreeCodes.add(bizCode);

        SysPermissionEntity root = ensureTreeNode(
                groupCode, 0, "ROOT",
                rootCode,
                groupName,
                groupName,
                null,
                product,
                0,
                now);

        int pSeq = productSeq(groupCode, product);
        SysPermissionEntity productNode = ensureTreeNode(
                groupCode,
                pSeq,
                "PRODUCT",
                productCode,
                productDisplay(product),
                groupName + "-" + productDisplay(product),
                root.getId(),
                product,
                pSeq,
                now);

        int bSeq = bizSeq(groupCode, bizLabel);
        SysPermissionEntity bizNode = ensureTreeNode(
                groupCode,
                bSeq,
                "BIZ",
                bizCode,
                bizLabel,
                groupName + "-" + productDisplay(product) + "-" + bizLabel,
                productNode.getId(),
                product,
                bSeq,
                now);

        String displayName = StringUtils.hasText(apiName) ? apiName : meta.code().trim();
        String treeName = groupName + "-" + productDisplay(product) + "-" + bizLabel + "-" + displayName;
        SysPermissionEntity api = upsertApiLeaf(
                meta.code().trim(),
                displayName,
                groupCode,
                product,
                treeName,
                bizNode.getId(),
                now);
        // 权限可能换组：先清旧组条目再绑定当前组
        permGroupItemMapper.deleteByPermissionId(api.getId());
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
        long expectedId = parsePermId(permNo);
        SysPermissionEntity existing = permissionMapper.selectByCode(code);
        if (existing == null) {
            existing = permissionMapper.selectByPermNo(permNo);
        }
        if (existing != null) {
            boolean standard = Objects.equals(existing.getId(), expectedId)
                    && permNo.equals(existing.getPermNo())
                    && groupCode.equals(existing.getGroupCode())
                    && nodeType.equals(existing.getNodeType());
            if (!standard) {
                deletePermissionCascade(existing.getId());
                existing = null;
            }
        }
        if (existing == null) {
            clearOccupant(expectedId, permNo);
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
            created.setId(expectedId);
            created.setCreateTime(now);
            created.setLastModifyTime(now);
            permissionMapper.insert(created);
            return created;
        }
        existing.setCode(code);
        existing.setName(name);
        existing.setGroupCode(groupCode);
        existing.setPermNo(permNo);
        existing.setParentId(parentId);
        existing.setProduct(product);
        existing.setTreeName(treeName);
        existing.setNodeType(nodeType);
        existing.setSortOrder(sortOrder);
        existing.setLastModifyTime(now);
        permissionMapper.updateById(existing);
        return existing;
    }

    private void clearOccupant(long expectedId, String permNo) {
        SysPermissionEntity byPermNo = permissionMapper.selectByPermNo(permNo);
        if (byPermNo != null) {
            deletePermissionCascade(byPermNo.getId());
        }
        SysPermissionEntity byId = permissionMapper.selectById(expectedId);
        if (byId != null) {
            deletePermissionCascade(byId.getId());
        }
    }

    private SysPermissionEntity upsertApiLeaf(String code,
                                              String name,
                                              String groupCode,
                                              String product,
                                              String treeName,
                                              Long parentId,
                                              Instant now) {
        SysPermissionEntity existing = permissionMapper.selectByCode(code);
        if (existing != null && (!isStandardApi(existing) || !groupCode.equals(existing.getGroupCode()))) {
            deletePermissionCascade(existing.getId());
            existing = null;
        }
        if (existing == null) {
            int leafSeq = nextLeafSeq(groupCode);
            String permNo = formatPermNo(groupCode, leafSeq);
            long id = parsePermId(permNo);
            int guard = 0;
            while ((permissionMapper.selectByPermNo(permNo) != null || permissionMapper.selectById(id) != null)
                    && guard++ < 100000) {
                leafSeq = nextLeafSeq(groupCode);
                permNo = formatPermNo(groupCode, leafSeq);
                id = parsePermId(permNo);
            }
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
            created.setId(id);
            created.setCreateTime(now);
            created.setLastModifyTime(now);
            permissionMapper.insert(created);
            return created;
        }
        existing.setName(name);
        existing.setGroupCode(groupCode);
        existing.setParentId(parentId);
        existing.setProduct(product);
        existing.setTreeName(treeName);
        existing.setNodeType("API");
        if (StringUtils.hasText(existing.getPermNo()) && existing.getPermNo().length() == 16) {
            try {
                existing.setSortOrder(Integer.parseInt(existing.getPermNo().substring(8)));
            } catch (Exception ignored) {
                // keep previous sort if perm_no malformed mid-update
            }
        }
        existing.setLastModifyTime(now);
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
            long id;
            try {
                id = Long.parseLong(groupCode);
            } catch (NumberFormatException ex) {
                id = snowflakeIdGenerator.nextId();
            }
            created.setId(id);
            created.setCreateTime(now);
            created.setLastModifyTime(now);
            permGroupMapper.insert(created);
            groupIdByCode.put(groupCode, created.getId());
            return created.getId();
        }
        existing.setName(groupName);
        existing.setProduct(product);
        existing.setSortOrder(sort);
        existing.setLastModifyTime(now);
        permGroupMapper.updateById(existing);
        groupIdByCode.put(groupCode, existing.getId());
        return existing.getId();
    }

    private void ensureGroupItem(Long groupId, Long permissionId, Instant now) {
        SysPermGroupItemEntity existing = permGroupItemMapper.selectByGroupAndPermission(groupId, permissionId);
        if (existing != null) {
            return;
        }
        // 同步开始前已有条目的组：视为已人工维护，不再回填扫描到的缺失 API
        if (groupsLockedFromRefill.contains(groupId)) {
            return;
        }
        SysPermGroupItemEntity item = new SysPermGroupItemEntity()
                .setId(snowflakeIdGenerator.nextId())
                .setGroupId(groupId)
                .setPermissionId(permissionId)
                .setCreateTime(now);
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
        for (Long groupId : allGroupIds) {
            if (rolePermGroupMapper.selectByRoleAndGroup(superAdmin.getId(), groupId) != null) {
                continue;
            }
            SysRolePermGroupEntity bind = new SysRolePermGroupEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setRoleId(superAdmin.getId())
                    .setGroupId(groupId)
                    .setCreateTime(now);
            rolePermGroupMapper.insert(bind);
        }
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
            case "system" -> "系统";
            case "platform" -> "平台";
            default -> product;
        };
    }

    private int productSeq(String groupCode, String product) {
        Map<String, Integer> map = productSeqByGroup.computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(product, k -> Math.min(productCounter.incrementAndGet(), 99));
    }

    private int bizSeq(String groupCode, String bizLabel) {
        Map<String, Integer> map = bizSeqByGroup.computeIfAbsent(groupCode, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(bizLabel, k -> 100 + Math.min(bizCounter.incrementAndGet(), 9899));
    }

    private int nextLeafSeq(String groupCode) {
        AtomicInteger counter = leafSeqByGroup.computeIfAbsent(groupCode, k -> new AtomicInteger(9999));
        return counter.incrementAndGet();
    }

    private record ApiSpec(PopedomGroup popedom, MiyfPermission meta, String bizLabel, String apiName) {
    }
}
