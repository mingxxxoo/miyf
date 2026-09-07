package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.QueryConditionHolder;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.redis.RedisJsonCache;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.constant.CacheKeys;
import cn.miyf.kitchen.repository.CategoryRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.security.SecurityUtils;
import cn.miyf.service.BaseApplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 菜品应用服务：用户端只读上架；管理端 CRUD、上下架、推荐与份数。
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
    private static final TypeReference<List<DishVo>> DISH_LIST_TYPE = new TypeReference<>() {
    };

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final RedisJsonCache redisJsonCache;
    private final RedisAppProperties redisAppProperties;
    private final KitchenCacheEvictService kitchenCacheEvictService;

    /**
     * 构造菜品服务。
     *
     * @param dishRepository           菜品仓储
     * @param categoryRepository       分类仓储
     * @param redisJsonCache           缓存
     * @param redisAppProperties       Redis 配置
     * @param kitchenCacheEvictService 失效服务
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public DishApplicationService(DishRepository dishRepository,
                                  CategoryRepository categoryRepository,
                                  RedisJsonCache redisJsonCache,
                                  RedisAppProperties redisAppProperties,
                                  KitchenCacheEvictService kitchenCacheEvictService) {
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
        this.redisJsonCache = redisJsonCache;
        this.redisAppProperties = redisAppProperties;
        this.kitchenCacheEvictService = kitchenCacheEvictService;
    }

    /**
     * 用户端分页：仅 ON_SALE。
     *
     * @param qo 查询条件
     * @return 分页 VO
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public PageResult<DishVo> pageUser(DishPageQo qo) {
        return QueryConditionHolder.run(qo, "d.is_recommend DESC, d.sort_order ASC, d.create_time DESC", () -> {
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            PageResult<Dish> result = dishRepository.pageUser(
                    qo.getCategoryId(), qo.getKeyword(), qo.getRecommend(), page, pageSize);
            return PageResult.of(result.records().stream().map(this::toVo).toList(),
                    result.total(), result.page(), result.pageSize());
        });
    }

    /**
     * 用户端详情：仅已上架可看。
     *
     * @param id 菜品 ID
     * @return 菜品 VO
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public DishVo getUserDetail(Long id) {
        Dish dish = requireById(dishRepository, id, "菜品不存在");
        requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.NOT_FOUND, "菜品不存在或未上架");
        return toVo(dish);
    }

    /**
     * 热门菜品：rating DESC, rating_count DESC（Redis 缓存）。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public List<DishVo> listHot(Integer limit) {
        int normalized = normalizeListLimit(limit);
        return redisJsonCache.getOrLoad(
                CacheKeys.dishesHot(normalized),
                DISH_LIST_TYPE,
                redisAppProperties.getCache().getHotDishTtlSeconds(),
                () -> dishRepository.findHot(normalized).stream().map(this::toVo).toList()
        );
    }

    /**
     * 推荐菜品列表（Redis 缓存）。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public List<DishVo> listRecommend(Integer limit) {
        int normalized = normalizeListLimit(limit);
        return redisJsonCache.getOrLoad(
                CacheKeys.dishesRecommend(normalized),
                DISH_LIST_TYPE,
                redisAppProperties.getCache().getHotDishTtlSeconds(),
                () -> dishRepository.findRecommend(normalized).stream().map(this::toVo).toList()
        );
    }

    /**
     * 管理端分页。
     *
     * @param qo 查询条件
     * @return 分页 VO
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
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
            PageResult<Dish> result = dishRepository.pageAdmin(
                    qo.getCategoryId(), qo.getStatus(), qo.getKeyword(), page, pageSize);
            return PageResult.of(result.records().stream().map(this::toVo).toList(),
                    result.total(), result.page(), result.pageSize());
        });
    }

    /**
     * 管理端详情（含草稿/下架）。
     *
     * @param id 菜品 ID
     * @return 菜品 VO
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public DishVo getAdminDetail(Long id) {
        return toVo(requireById(dishRepository, id, "菜品不存在"));
    }

    /**
     * 创建菜品，初始状态为 DRAFT。
     *
     * @param dto 请求
     * @return 新建菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public DishVo create(DishSaveDto dto) {
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        Long adminId = SecurityUtils.currentAdminId();
        Dish dish = new Dish();
        applyDto(dish, dto, true);
        dish.setStatus("DRAFT");
        dish.setRating(BigDecimal.ZERO);
        dish.setRatingCount(0);
        dish.setCreatedBy(adminId);
        dish.setUpdatedBy(adminId);
        DishVo vo = toVo(save(dishRepository, dish));
        kitchenCacheEvictService.evictDishBrowse();
        return vo;
    }

    /**
     * 更新菜品基础信息（不改状态与评分）。
     *
     * @param id  菜品 ID
     * @param dto 请求
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public DishVo update(Long id, DishSaveDto dto) {
        Dish dish = requireById(dishRepository, id, "菜品不存在");
        validateCategory(dto.getCategoryId());
        validateStock(dto.getStockType(), dto.getStock());
        applyDto(dish, dto, false);
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        DishVo vo = toVo(save(dishRepository, dish));
        kitchenCacheEvictService.evictDishBrowse();
        return vo;
    }

    /**
     * 上架：DRAFT / OFF_SALE → ON_SALE。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public DishVo publish(Long id) {
        Dish dish = requireById(dishRepository, id, "菜品不存在");
        requireTrue(!"ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "菜品已上架");
        requireTrue(dish.getCategoryId() != null, ErrorCode.BAD_REQUEST, "上架前请先选择分类");
        validateCategory(dish.getCategoryId());
        if ("LIMITED".equals(dish.getStockType())) {
            requireTrue(dish.getStock() >= 0, ErrorCode.BAD_REQUEST, "可提供份数无效");
        }
        dish.setStatus("ON_SALE");
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        // 上架不改图集：置 null 避免 save 触发 replaceImages
        dish.setImages(null);
        DishVo vo = toVo(save(dishRepository, dish));
        kitchenCacheEvictService.evictDishBrowse();
        return vo;
    }

    /**
     * 下架：ON_SALE → OFF_SALE。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public DishVo unpublish(Long id) {
        Dish dish = requireById(dishRepository, id, "菜品不存在");
        requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "仅上架菜品可下架");
        dish.setStatus("OFF_SALE");
        dish.setUpdatedBy(SecurityUtils.currentAdminId());
        dish.setImages(null);
        DishVo vo = toVo(save(dishRepository, dish));
        kitchenCacheEvictService.evictDishBrowse();
        return vo;
    }

    /**
     * 物理删除菜品；有预约历史时禁止删除。
     *
     * @param id 菜品 ID
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        requireById(dishRepository, id, "菜品不存在");
        if (dishRepository.hasOrderHistory(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "有预约历史的菜品无法删除");
        }
        deleteById(dishRepository, id);
        kitchenCacheEvictService.evictDishBrowse();
    }

    /**
     * 校验分类存在且启用。
     *
     * @param categoryId 分类 ID，可空（草稿允许暂不绑分类）
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        var category = requireById(categoryRepository, categoryId, "分类不存在");
        requireTrue("ENABLED".equals(category.getStatus()), ErrorCode.BAD_REQUEST, "分类已停用");
    }

    /**
     * 校验份数模式与份数。
     *
     * @param stockType 模式
     * @param stock     份数
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
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
     *
     * @param dish     菜品
     * @param dto      请求
     * @param creating 是否新建
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    private void applyDto(Dish dish, DishSaveDto dto, boolean creating) {
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName().trim());
        dish.setSubtitle(dto.getSubtitle());
        dish.setDescription(dto.getDescription());
        dish.setCoverImage(dto.getCoverImage());
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
        dish.setImages(dto.getImages());
    }

    /**
     * 规范化热门/推荐条数。
     *
     * @param limit 请求条数
     * @return 合法条数
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
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
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    private DishVo toVo(Dish dish) {
        return new DishVo()
                .setId(dish.getId())
                .setCategoryId(dish.getCategoryId())
                .setCategoryName(dish.getCategoryName())
                .setName(dish.getName())
                .setSubtitle(dish.getSubtitle())
                .setDescription(dish.getDescription())
                .setCoverImage(dish.getCoverImage())
                .setStatus(dish.getStatus())
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
