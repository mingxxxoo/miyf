package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.infrastructure.cache.ListCache;
import cn.miyf.kitchen.bean.dto.CategorySaveDto;
import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Category;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.constant.KitchenCacheKeys;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.CategoryRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 分类应用服务：用户端只读启用列表；厨师端按厨房 CRUD；管理端全量维护已下线入口。
 * 用户端 listEnabled 经 {@link KitchenAccessService#requireChefOrBound()} 门控，并按当前厨房过滤。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Service
public class CategoryApplicationService extends BaseApplicationService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ENABLED", "DISABLED");

    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final KitchenRepository kitchenRepository;
    private final ListCache<CategoryVo> categoryListCache;
    private final RedisAppProperties redisAppProperties;
    private final KitchenCacheEvictService kitchenCacheEvictService;
    private final KitchenAccessService kitchenAccessService;

    /**
     * 构造分类服务。
     *
     * @param categoryRepository       分类仓储
     * @param dishRepository           菜品仓储（删除前校验引用）
     * @param kitchenRepository        厨房仓储
     * @param cacheClient              缓存门面
     * @param redisAppProperties       Redis 配置
     * @param kitchenCacheEvictService 失效服务
     * @param kitchenAccessService     身份与绑定门控
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public CategoryApplicationService(CategoryRepository categoryRepository,
                                      DishRepository dishRepository,
                                      KitchenRepository kitchenRepository,
                                      CacheClient cacheClient,
                                      RedisAppProperties redisAppProperties,
                                      KitchenCacheEvictService kitchenCacheEvictService,
                                      KitchenAccessService kitchenAccessService) {
        this.categoryRepository = categoryRepository;
        this.dishRepository = dishRepository;
        this.kitchenRepository = kitchenRepository;
        this.categoryListCache = cacheClient.lists(CategoryVo.class);
        this.redisAppProperties = redisAppProperties;
        this.kitchenCacheEvictService = kitchenCacheEvictService;
        this.kitchenAccessService = kitchenAccessService;
    }

    /**
     * 用户端：当前厨房启用中的分类列表（列表缓存）。厨师或已绑定食客可访问。
     *
     * @return 分类 VO 列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<CategoryVo> listEnabled() {
        kitchenAccessService.requireChefOrBound();
        Long kitchenId = resolveViewerKitchenId();
        if (kitchenId == null) {
            return List.of();
        }
        Duration ttl = Duration.ofSeconds(redisAppProperties.getCache().getCategoryTtlSeconds());
        return categoryListCache.getOrLoadList(
                KitchenCacheKeys.categoriesEnabled(kitchenId),
                ttl,
                () -> categoryRepository.selectEnabledByKitchenId(kitchenId).stream()
                        .map(EntityConverters::toCategory)
                        .map(this::toVo)
                        .toList()
        );
    }

    /**
     * 厨师端：本厨房全部分类（含停用）。
     *
     * @return 分类列表
     */
    public List<CategoryVo> listChef() {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        return categoryRepository.selectByKitchenId(kitchenId).stream()
                .map(EntityConverters::toCategory)
                .map(this::toVo)
                .toList();
    }

    /**
     * 厨师端：创建本厨房分类。
     *
     * @param dto 请求
     * @return 新建分类
     */
    @Transactional
    public CategoryVo createChef(CategorySaveDto dto) {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        Category category = new Category();
        category.setKitchenId(kitchenId);
        applyDto(category, dto, true);
        CategoryEntity entity = EntityConverters.toCategoryEntity(category);
        insert(categoryRepository, entity);
        CategoryVo vo = toVo(EntityConverters.toCategory(entity));
        kitchenCacheEvictService.evictCategories(kitchenId);
        return vo;
    }

    /**
     * 厨师端：更新本厨房分类。
     *
     * @param id  分类 ID
     * @param dto 请求
     * @return 更新后分类
     */
    @Transactional
    public CategoryVo updateChef(Long id, CategorySaveDto dto) {
        Category category = requireChefCategory(id);
        applyDto(category, dto, false);
        CategoryEntity entity = EntityConverters.toCategoryEntity(category);
        update(categoryRepository, entity);
        CategoryVo vo = toVo(EntityConverters.toCategory(entity));
        kitchenCacheEvictService.evictCategories(category.getKitchenId());
        return vo;
    }

    /**
     * 厨师端：删除本厨房分类；仍有菜品时拒绝。
     *
     * @param id 分类 ID
     */
    @Transactional
    public void deleteChef(Long id) {
        Category category = requireChefCategory(id);
        if (dishRepository.countByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下仍有菜品，无法删除");
        }
        deleteById(categoryRepository, id);
        kitchenCacheEvictService.evictCategories(category.getKitchenId());
    }

    /**
     * 管理端：全部分类（含停用）。
     *
     * @return 分类 VO 列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<CategoryVo> listAll() {
        return categoryRepository.selectAllOrdered().stream()
                .map(EntityConverters::toCategory)
                .map(this::toVo)
                .toList();
    }

    /**
     * 管理端：创建分类。
     *
     * @param dto 请求
     * @return 新建分类
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public CategoryVo create(CategorySaveDto dto) {
        Category category = new Category();
        applyDto(category, dto, true);
        CategoryEntity entity = EntityConverters.toCategoryEntity(category);
        insert(categoryRepository, entity);
        CategoryVo vo = toVo(EntityConverters.toCategory(entity));
        kitchenCacheEvictService.evictCategories(category.getKitchenId());
        return vo;
    }

    /**
     * 管理端：更新分类（含排序、启停）。
     *
     * @param id  分类 ID
     * @param dto 请求
     * @return 更新后分类
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public CategoryVo update(Long id, CategorySaveDto dto) {
        Category category = EntityConverters.toCategory(requireById(categoryRepository, id, "分类不存在"));
        applyDto(category, dto, false);
        CategoryEntity entity = EntityConverters.toCategoryEntity(category);
        update(categoryRepository, entity);
        CategoryVo vo = toVo(EntityConverters.toCategory(entity));
        kitchenCacheEvictService.evictCategories(category.getKitchenId());
        return vo;
    }

    /**
     * 管理端：物理删除分类；仍有菜品时拒绝。
     *
     * @param id 分类 ID
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        Category category = EntityConverters.toCategory(requireById(categoryRepository, id, "分类不存在"));
        if (dishRepository.countByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下仍有菜品，无法删除");
        }
        deleteById(categoryRepository, id);
        kitchenCacheEvictService.evictCategories(category.getKitchenId());
    }

    private Category requireChefCategory(Long id) {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        Category category = EntityConverters.toCategory(requireById(categoryRepository, id, "分类不存在"));
        requireTrue(kitchenId.equals(category.getKitchenId()), ErrorCode.FORBIDDEN, "无权操作该分类");
        return category;
    }

    /**
     * 解析当前浏览者所属厨房：食客取绑定厨；厨师取自有厨（未建厨返回 null）。
     */
    private Long resolveViewerKitchenId() {
        UserEntity user = kitchenAccessService.requireUser();
        if (Boolean.TRUE.equals(user.getDiner()) && "DINER".equals(user.getActiveRole())) {
            return kitchenAccessService.requireBoundKitchen().getId();
        }
        if (Boolean.TRUE.equals(user.getChef())) {
            KitchenEntity owned = kitchenRepository.selectByOwnerUserId(user.getId());
            return owned == null ? null : owned.getId();
        }
        if (Boolean.TRUE.equals(user.getDiner())) {
            return kitchenAccessService.requireBoundKitchen().getId();
        }
        return null;
    }

    /**
     * 将 DTO 应用到领域对象。
     *
     * @param category 领域对象
     * @param dto      请求
     * @param creating 是否新建
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private void applyDto(Category category, CategorySaveDto dto, boolean creating) {
        category.setName(dto.getName().trim());
        category.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        } else if (creating) {
            category.setSortOrder(0);
        }
        String status = dto.getStatus();
        if (status == null || status.isBlank()) {
            if (creating) {
                category.setStatus("ENABLED");
            }
        } else {
            String normalized = status.trim().toUpperCase();
            requireTrue(ALLOWED_STATUS.contains(normalized), ErrorCode.BAD_REQUEST, "分类状态无效");
            category.setStatus(normalized);
        }
    }

    /**
     * 领域转 VO。
     *
     * @param category 分类
     * @return VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private CategoryVo toVo(Category category) {
        return new CategoryVo()
                .setId(category.getId())
                .setKitchenId(category.getKitchenId())
                .setName(category.getName())
                .setIcon(category.getIcon())
                .setSortOrder(category.getSortOrder())
                .setStatus(category.getStatus())
                .setLastModifyTime(category.getLastModifyTime());
    }
}
