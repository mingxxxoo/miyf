package cn.miyf.kitchen.service;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.QueryConditionHolder;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.infrastructure.cache.ListCache;
import cn.miyf.kitchen.bean.document.DishSearchDocument;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.DishImageEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.CategoryRepository;
import cn.miyf.kitchen.repository.DishImageRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.search.DishSearchIndexService;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜品应用服务：用户端只读上架；管理端 CRUD、上下架、推荐与份数。
 * 微信端列表/热门/推荐优先 ES 召回（{@link DishSearchIndexService#isRecallEnabled()}），否则回落 SQL。
 * 数据访问直接使用 {@link DishRepository}/{@link DishImageRepository}（Mapper 接口），
 * 图集替换、分类名 enrich 与默认字段补齐在本服务编排。
 * 胡闹厨房：厨师端建菜/提审/上下架，须归属本厨且关键字段变更后重新审核。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Service
public class DishApplicationService extends BaseApplicationService {

    private static final Set<String> STOCK_TYPES = Set.of("LIMITED", "UNLIMITED");
    private static final Set<String> ADMIN_STATUS = Set.of("DRAFT", "ON_SALE", "OFF_SALE");
    private static final int DEFAULT_LIST_LIMIT = 10;
    private static final int MAX_LIST_LIMIT = 50;

    private final DishRepository dishRepository;
    private final DishImageRepository dishImageRepository;
    private final CategoryRepository categoryRepository;
    private final ListCache<DishVo> dishListCache;
    private final RedisAppProperties redisAppProperties;
    private final KitchenCacheEvictService kitchenCacheEvictService;
    private final DishSearchIndexService dishSearchIndexService;
    private final KitchenAccessService kitchenAccessService;
    private final DishAuditWorkflowService dishAuditWorkflowService;

    public DishApplicationService(DishRepository dishRepository,
                                  DishImageRepository dishImageRepository,
                                  CategoryRepository categoryRepository,
                                  CacheClient cacheClient,
                                  RedisAppProperties redisAppProperties,
                                  KitchenCacheEvictService kitchenCacheEvictService,
                                  DishSearchIndexService dishSearchIndexService,
                                  KitchenAccessService kitchenAccessService,
                                  DishAuditWorkflowService dishAuditWorkflowService) {
        this.dishRepository = dishRepository;
        this.dishImageRepository = dishImageRepository;
        this.categoryRepository = categoryRepository;
        this.dishListCache = cacheClient.lists(DishVo.class);
        this.redisAppProperties = redisAppProperties;
        this.kitchenCacheEvictService = kitchenCacheEvictService;
        this.dishSearchIndexService = dishSearchIndexService;
        this.kitchenAccessService = kitchenAccessService;
        this.dishAuditWorkflowService = dishAuditWorkflowService;
    }

    /**
     * 用户端分页：须已绑定且厨房 OPEN；仅 ON_SALE。
     * ES 召回可用时走索引再回表，否则 SQL。
     *
     * @param qo 查询条件
     * @return 分页 VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public PageResult<DishVo> pageUser(DishPageQo qo) {
        Long kitchenId = kitchenAccessService.requireBoundKitchen().getId();
        return QueryConditionHolder.run(qo, DishSearchDocument.DEFAULT_ORDER, () -> {
            long page = qo.normalizedPage();
            long pageSize = qo.normalizedRows();
            long off = offset(page, pageSize);
            List<DishVo> records = dishRepository.selectUserPage(
                            kitchenId, qo.getCategoryId(), qo.getKeyword(), qo.getRecommend(), off, pageSize)
                    .stream().map(e -> toVo(enrich(EntityConverters.toDish(e)))).toList();
            long total = dishRepository.countUserPage(
                    kitchenId, qo.getCategoryId(), qo.getKeyword(), qo.getRecommend());
            return PageResult.of(records, total, page, pageSize);
        });
    }

    /**
     * 用户端详情：仅已上架可看。
     *
     * @param id 菜品 ID
     * @return 菜品 VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public DishVo getUserDetail(Long id) {
        Dish dish = requireDish(id);
        kitchenAccessService.requireBoundToDish(dish);
        requireTrue("ON_SALE".equals(dish.getStatus()) && "APPROVED".equals(dish.getAuditStatus()),
                ErrorCode.NOT_FOUND, "菜品不存在或未上架");
        return toVo(dish);
    }

    /**
     * 热门菜品：rating DESC, rating_count DESC（列表缓存；ES 可用则召回）。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<DishVo> listHot(Integer limit) {
        Long kitchenId = kitchenAccessService.requireBoundKitchen().getId();
        int normalized = normalizeListLimit(limit);
        return dishRepository.selectHot(kitchenId, normalized).stream()
                .map(e -> toVo(enrich(EntityConverters.toDish(e)))).toList();
    }

    /**
     * 推荐菜品列表（列表缓存；ES 可用则召回）。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<DishVo> listRecommend(Integer limit) {
        Long kitchenId = kitchenAccessService.requireBoundKitchen().getId();
        int normalized = normalizeListLimit(limit);
        return dishRepository.selectRecommend(kitchenId, normalized).stream()
                .map(e -> toVo(enrich(EntityConverters.toDish(e)))).toList();
    }

    /**
     * 管理端分页。
     *
     * @param qo 查询条件
     * @return 分页 VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public PageResult<DishVo> pageAdmin(DishPageQo qo) {
        if (qo.getStatus() != null && !qo.getStatus().isBlank()) {
            String status = qo.getStatus().trim().toUpperCase();
            requireTrue(ADMIN_STATUS.contains(status), ErrorCode.BAD_REQUEST, "菜品状态无效");
            qo.setStatus(status);
        }
        return QueryConditionHolder.run(qo, "d.sort_order ASC, d.last_modify_time DESC", () -> {
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            long off = offset(page, pageSize);
            List<DishVo> records = dishRepository.selectAdminPage(
                            qo.getKitchenId(), qo.getCategoryId(), qo.getStatus(), qo.getAuditStatus(),
                            qo.getKeyword(), off, pageSize)
                    .stream().map(e -> toVo(enrich(EntityConverters.toDish(e)))).toList();
            long total = dishRepository.countAdminPage(
                    qo.getKitchenId(), qo.getCategoryId(), qo.getStatus(), qo.getAuditStatus(), qo.getKeyword());
            return PageResult.of(records, total, page, pageSize);
        });
    }

    /**
     * 管理端详情（含草稿/下架）。
     *
     * @param id 菜品 ID
     * @return 菜品 VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public DishVo getAdminDetail(Long id) {
        return toVo(requireDish(id));
    }

    /**
     * 创建菜品，初始状态为 DRAFT。
     *
     * @param dto 请求
     * @return 新建菜品
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public DishVo create(DishSaveDto dto) {
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        Long adminId = SecurityUtils.currentAdminId();
        Dish dish = new Dish();
        applyDto(dish, dto, true);
        KitchenEntity kitchen = kitchenAccessService.requireAssignableKitchen(dto.getKitchenId());
        dish.setKitchenId(kitchen.getId());
        dish.setStatus("DRAFT");
        dish.setAuditStatus("DRAFT");
        dish.setRating(BigDecimal.ZERO);
        dish.setRatingCount(0);
        dish.setCreatedBy(adminId);
        dish.setUpdatedBy(adminId);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 更新菜品基础信息（不改状态与评分）；刷新浏览缓存（索引随 search.enabled 同步）。
     *
     * @param id  菜品 ID
     * @param dto 请求
     * @return 更新后菜品
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public DishVo update(Long id, DishSaveDto dto) {
        Dish dish = requireDish(id);
        if ("PENDING_REVIEW".equals(dish.getAuditStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "审核中不可编辑，请等待结果或撤回后修改");
        }
        boolean keyChanged = isKeyFieldChanged(dish, dto);
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        applyDto(dish, dto, false);
        if (dto.getImages() == null) {
            dish.setImages(null);
        }
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        if (keyChanged) {
            dish.setAuditStatus("DRAFT");
            if ("ON_SALE".equals(dish.getStatus())) {
                dish.setStatus("OFF_SALE");
            }
        }
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 管理端提交菜品审核（Flowable）。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo submitAdminAudit(Long id) {
        Dish dish = requireDish(id);
        requireTrue(dish.getKitchenId() != null, ErrorCode.BAD_REQUEST, "菜品未归属厨房");
        requireTrue(dish.getCategoryId() != null, ErrorCode.BAD_REQUEST, "提交审核前请先选择分类");
        DishEntity entity = requireById(dishRepository, id, "菜品不存在");
        dishAuditWorkflowService.submit(entity);
        kitchenCacheEvictService.evictDishBrowse();
        return getAdminDetail(id);
    }

    /**
     * 上架：须已审核通过；OFF_SALE/DRAFT → ON_SALE。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public DishVo publish(Long id) {
        Dish dish = requireDish(id);
        requireTrue("APPROVED".equals(dish.getAuditStatus()), ErrorCode.INVALID_STATUS, "审核通过后才能上架");
        requireTrue(!"ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "菜品已上架");
        requireTrue(dish.getCategoryId() != null, ErrorCode.BAD_REQUEST, "上架前请先选择分类");
        validateCategory(dish.getCategoryId());
        if ("LIMITED".equals(dish.getStockType())) {
            requireTrue(dish.getStock() >= 0, ErrorCode.BAD_REQUEST, "可提供份数无效");
        }
        dish.setStatus("ON_SALE");
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        // 上架不改图集：置 null 避免 persist 触发 replaceImages
        dish.setImages(null);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 下架：ON_SALE → OFF_SALE（写库 + 失效浏览缓存；ES 随召回开关同步）。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public DishVo unpublish(Long id) {
        Dish dish = requireDish(id);
        requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "仅上架菜品可下架");
        dish.setStatus("OFF_SALE");
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        dish.setImages(null);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 物理删除菜品；有预约历史时禁止删除。
     *
     * @param id 菜品 ID
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        requireById(dishRepository, id, "菜品不存在");
        if (dishRepository.countOrderItems(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "有预约历史的菜品无法删除");
        }
        dishImageRepository.deleteByDishId(id);
        deleteById(dishRepository, id);
        kitchenCacheEvictService.evictDishBrowse();
    }

    /**
     * 厨师端本厨房菜品分页。
     *
     * @param qo 状态、审核状态、关键字与分页
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public PageResult<DishVo> pageChef(DishPageQo qo) {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        long page = pageOf(qo);
        long pageSize = pageSizeOf(qo);
        List<DishVo> records = dishRepository.selectChefPage(
                        kitchenId, qo.getStatus(), qo.getAuditStatus(), qo.getKeyword(),
                        offset(page, pageSize), pageSize)
                .stream().map(e -> toVo(enrich(EntityConverters.toDish(e)))).toList();
        long total = dishRepository.countChefPage(kitchenId, qo.getStatus(), qo.getAuditStatus(), qo.getKeyword());
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * 厨师端菜品详情（须归属本厨）。
     *
     * @param id 菜品 ID
     * @return 菜品 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public DishVo getChefDetail(Long id) {
        return toVo(requireChefDish(id));
    }

    /**
     * 厨师创建菜品草稿（归属本厨，auditStatus=DRAFT）。
     *
     * @param dto 菜品内容
     * @return 新建菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo createChef(DishSaveDto dto) {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        Long userId = SecurityUtils.currentUserId();
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        Dish dish = new Dish();
        applyDto(dish, dto, true);
        dish.setKitchenId(kitchenId);
        dish.setStatus("DRAFT");
        dish.setAuditStatus("DRAFT");
        dish.setRating(BigDecimal.ZERO);
        dish.setRatingCount(0);
        dish.setCreatedBy(userId);
        dish.setUpdatedBy(userId);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 厨师更新菜品；审核中不可改，关键字段变更则回退为草稿并下架。
     *
     * @param id  菜品 ID
     * @param dto 菜品内容
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo updateChef(Long id, DishSaveDto dto) {
        Dish dish = requireChefDish(id);
        if ("PENDING_REVIEW".equals(dish.getAuditStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "审核中不可编辑");
        }
        boolean keyChanged = isKeyFieldChanged(dish, dto);
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        applyDto(dish, dto, false);
        if (dto.getImages() == null) {
            dish.setImages(null);
        }
        dish.setUpdatedBy(SecurityUtils.currentUserId());
        if (keyChanged) {
            dish.setAuditStatus("DRAFT");
            if ("ON_SALE".equals(dish.getStatus())) {
                dish.setStatus("OFF_SALE");
            }
        }
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 厨师提交菜品审核（Flowable）。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo submitChefAudit(Long id) {
        Dish dish = requireChefDish(id);
        requireTrue(dish.getCategoryId() != null, ErrorCode.BAD_REQUEST, "提交审核前请先选择分类");
        DishEntity entity = requireById(dishRepository, id, "菜品不存在");
        dishAuditWorkflowService.submit(entity);
        kitchenCacheEvictService.evictDishBrowse();
        return getChefDetail(id);
    }

    /**
     * 厨师撤回审核中的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo withdrawChefAudit(Long id) {
        requireChefDish(id);
        DishEntity entity = requireById(dishRepository, id, "菜品不存在");
        dishAuditWorkflowService.withdraw(entity);
        kitchenCacheEvictService.evictDishBrowse();
        return getChefDetail(id);
    }

    /**
     * 管理端撤回审核中的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo withdrawAdminAudit(Long id) {
        requireDish(id);
        DishEntity entity = requireById(dishRepository, id, "菜品不存在");
        dishAuditWorkflowService.withdraw(entity);
        kitchenCacheEvictService.evictDishBrowse();
        return getAdminDetail(id);
    }

    /**
     * 厨师上架：须已审核通过。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo publishChef(Long id) {
        Dish dish = requireChefDish(id);
        requireTrue("APPROVED".equals(dish.getAuditStatus()), ErrorCode.INVALID_STATUS, "审核通过后才能上架");
        requireTrue(!"ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "菜品已上架");
        dish.setStatus("ON_SALE");
        dish.setUpdatedBy(SecurityUtils.currentUserId());
        dish.setImages(null);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /**
     * 厨师下架本厨房上架菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public DishVo unpublishChef(Long id) {
        Dish dish = requireChefDish(id);
        requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "仅上架菜品可下架");
        dish.setStatus("OFF_SALE");
        dish.setUpdatedBy(SecurityUtils.currentUserId());
        dish.setImages(null);
        Dish saved = persistDish(dish);
        kitchenCacheEvictService.evictDishBrowse();
        return toVo(saved);
    }

    /** 校验菜品归属当前厨师厨房。 */
    private Dish requireChefDish(Long id) {
        Dish dish = requireDish(id);
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        requireTrue(kitchenId.equals(dish.getKitchenId()), ErrorCode.FORBIDDEN, "无权操作该菜品");
        return dish;
    }

    /**
     * 名称/简介/封面/分类/单位等关键字段是否变更；变更后须重新审核。
     * null 表示本次不改该字段，不计入关键变更。
     */
    private boolean isKeyFieldChanged(Dish dish, DishSaveDto dto) {
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (!java.util.Objects.equals(dish.getName(), name)) {
                return true;
            }
        }
        // null = 本次不改该字段，不计入关键变更（避免小程序部分更新误触发重审）
        if (dto.getSubtitle() != null
                && !java.util.Objects.equals(blankToNull(dish.getSubtitle()), blankToNull(dto.getSubtitle()))) {
            return true;
        }
        if (dto.getDescription() != null
                && !java.util.Objects.equals(blankToNull(dish.getDescription()), blankToNull(dto.getDescription()))) {
            return true;
        }
        if (dto.getCoverImage() != null
                && !java.util.Objects.equals(blankToNull(dish.getCoverImage()), blankToNull(dto.getCoverImage()))) {
            return true;
        }
        if (dto.getCategoryId() != null && !java.util.Objects.equals(dish.getCategoryId(), dto.getCategoryId())) {
            return true;
        }
        if (dto.getUnit() != null && !dto.getUnit().isBlank()
                && !java.util.Objects.equals(dish.getUnit(), dto.getUnit().trim())) {
            return true;
        }
        return dto.getImages() != null && !java.util.Objects.equals(dish.getImages(), dto.getImages());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 按 ES 命中 ID 顺序回表（过滤非上架或已删）。
     *
     * @param idStrings 文档 ID
     * @return VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    private List<DishVo> hydrateByIds(List<String> idStrings) {
        if (idStrings == null || idStrings.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(idStrings.size());
        for (String raw : idStrings) {
            try {
                ids.add(Long.valueOf(raw));
            } catch (NumberFormatException ignored) {
                // 非法文档 ID 跳过
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Dish> byId = new LinkedHashMap<>();
        List<DishEntity> entities = dishRepository.selectBatchIds(ids);
        for (DishEntity entity : entities) {
            Dish dish = enrich(EntityConverters.toDish(entity));
            if (dish != null && dish.getId() != null && "ON_SALE".equals(dish.getStatus())) {
                byId.put(dish.getId(), dish);
            }
        }
        List<DishVo> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Dish dish = byId.get(id);
            if (dish != null) {
                result.add(toVo(dish));
            }
        }
        return result;
    }

    private Dish requireDish(Long id) {
        return enrich(EntityConverters.toDish(requireById(dishRepository, id, "菜品不存在")));
    }

    /**
     * 保存菜品主表；images 非 null 时同步替换图集。
     * 新建时补齐评分/库存/状态默认值，避免空字段入库。
     *
     * @param dish 菜品领域对象
     * @return 丰富后的菜品（含分类名与图集）
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Dish persistDish(Dish dish) {
        DishEntity entity = EntityConverters.toDishEntity(dish);
        // 仅新建时写默认值，更新保留已有评分与状态
        if (entity.getId() == null) {
            if (entity.getRating() == null) {
                entity.setRating(BigDecimal.ZERO);
            }
            if (entity.getRatingCount() == null) {
                entity.setRatingCount(0);
            }
            if (entity.getStock() == null) {
                entity.setStock(0);
            }
            if (entity.getStockType() == null) {
                entity.setStockType("LIMITED");
            }
            if (entity.getStatus() == null) {
                entity.setStatus("DRAFT");
            }
            if (entity.getAuditStatus() == null) {
                entity.setAuditStatus("DRAFT");
            }
        }
        save(dishRepository, entity);
        dish.setId(entity.getId());
        // null 表示本次不改图集；空列表表示清空
        if (dish.getImages() != null) {
            replaceImages(entity.getId(), dish.getImages());
        }
        return enrich(EntityConverters.toDish(dishRepository.selectById(entity.getId())));
    }

    /**
     * 替换菜品图集：先删后按顺序插入，保证 sort_order 稳定。
     *
     * @param dishId    菜品 ID
     * @param imageUrls 图片 URL；空列表表示清空
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private void replaceImages(Long dishId, List<String> imageUrls) {
        dishImageRepository.deleteByDishId(dishId);
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        int sort = 0;
        Instant now = Instant.now();
        for (String url : imageUrls) {
            DishImageEntity img = new DishImageEntity();
            img.setDishId(dishId);
            img.setUrl(url);
            img.setSortOrder(sort++);
            img.setCreateTime(now);
            dishImageRepository.insert(img);
        }
    }

    /**
     * 补充分类名与图片 URL 列表，供展示层直接使用。
     *
     * @param dish 菜品
     * @return 丰富后的菜品
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Dish enrich(Dish dish) {
        if (dish == null) {
            return null;
        }
        if (dish.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.selectById(dish.getCategoryId());
            if (category != null) {
                dish.setCategoryName(category.getName());
            }
        }
        List<DishImageEntity> images = dishImageRepository.selectByDishId(dish.getId());
        dish.setImages(images.stream().map(DishImageEntity::getUrl).collect(Collectors.toCollection(ArrayList::new)));
        return dish;
    }

    /**
     * 校验分类存在且启用。
     *
     * @param categoryId 分类 ID，可空（草稿允许暂不绑分类）
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CategoryEntity category = requireById(categoryRepository, categoryId, "分类不存在");
        requireTrue("ENABLED".equals(category.getStatus()), ErrorCode.BAD_REQUEST, "分类已停用");
    }

    /**
     * 校验份数模式与份数。
     *
     * @param stockType 模式
     * @param stock     份数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private void validateStock(String stockType, Integer stock) {
        requireTrue(stockType != null && STOCK_TYPES.contains(stockType.trim().toUpperCase()),
                ErrorCode.BAD_REQUEST, "份数模式无效");
        if ("LIMITED".equalsIgnoreCase(stockType) && stock != null) {
            requireTrue(stock >= 0, ErrorCode.BAD_REQUEST, "可提供份数不能为负");
        }
    }

    /**
     * 将 DTO 应用到领域对象。
     * <p>
     * 更新时：字符串/分类字段为 null 表示不修改；空字符串表示清空。
     * 图集：null 不改，空列表清空。库存字段每次按请求覆盖（免审）。
     *
     * @param dish     菜品
     * @param dto      请求
     * @param creating 是否新建
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private void applyDto(Dish dish, DishSaveDto dto, boolean creating) {
        if (creating || dto.getCategoryId() != null) {
            dish.setCategoryId(dto.getCategoryId());
        }
        if (dto.getName() != null) {
            dish.setName(dto.getName().trim());
        }
        if (creating || dto.getSubtitle() != null) {
            dish.setSubtitle(blankToNull(dto.getSubtitle()));
        }
        if (creating || dto.getDescription() != null) {
            dish.setDescription(blankToNull(dto.getDescription()));
        }
        if (creating || dto.getCoverImage() != null) {
            dish.setCoverImage(blankToNull(dto.getCoverImage()));
        }
        if (dto.getSortOrder() != null) {
            dish.setSortOrder(dto.getSortOrder());
        } else if (creating) {
            dish.setSortOrder(0);
        }
        if (dto.getRecommend() != null) {
            dish.setRecommend(dto.getRecommend());
        } else if (creating) {
            dish.setRecommend(false);
        }
        String stockType = dto.getStockType().trim().toUpperCase();
        dish.setStockType(stockType);
        if ("UNLIMITED".equals(stockType)) {
            dish.setStock(0);
        } else {
            dish.setStock(dto.getStock() == null ? 0 : dto.getStock());
        }
        if (dto.getUnit() != null && !dto.getUnit().isBlank()) {
            dish.setUnit(dto.getUnit().trim());
        } else if (creating) {
            dish.setUnit("份");
        }
        // null 表示本次不改图集；空列表表示清空
        if (creating || dto.getImages() != null) {
            dish.setImages(dto.getImages());
        }
    }

    /**
     * 规范化热门/推荐条数。
     *
     * @param limit 请求条数
     * @return 合法条数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private int normalizeListLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    /**
     * 领域转 VO。
     *
     * @param dish 菜品
     * @return VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private DishVo toVo(Dish dish) {
        return new DishVo()
                .setId(dish.getId())
                .setCategoryId(dish.getCategoryId())
                .setKitchenId(dish.getKitchenId())
                .setCategoryName(dish.getCategoryName())
                .setName(dish.getName())
                .setSubtitle(dish.getSubtitle())
                .setDescription(dish.getDescription())
                .setCoverImage(dish.getCoverImage())
                .setStatus(dish.getStatus())
                .setAuditStatus(dish.getAuditStatus())
                .setRejectReason(dish.getRejectReason())
                .setRecommend(dish.isRecommend())
                .setStock(dish.getStock())
                .setStockType(dish.getStockType())
                .setUnit(dish.getUnit())
                .setSortOrder(dish.getSortOrder())
                .setRating(dish.getRating())
                .setRatingCount(dish.getRatingCount())
                .setImages(dish.getImages());
    }
}
