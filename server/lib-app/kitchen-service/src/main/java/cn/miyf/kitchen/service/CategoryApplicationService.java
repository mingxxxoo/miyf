package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.redis.RedisJsonCache;
import cn.miyf.kitchen.bean.dto.CategorySaveDto;
import cn.miyf.kitchen.bean.model.Category;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.constant.CacheKeys;
import cn.miyf.kitchen.repository.CategoryRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.service.BaseApplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 分类应用服务：用户端只读启用列表；管理端 CRUD / 启停 / 排序。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Service
public class CategoryApplicationService extends BaseApplicationService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ENABLED", "DISABLED");
    private static final TypeReference<List<CategoryVo>> CATEGORY_LIST_TYPE = new TypeReference<>() {
    };

    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final RedisJsonCache redisJsonCache;
    private final RedisAppProperties redisAppProperties;
    private final KitchenCacheEvictService kitchenCacheEvictService;

    /**
     * 构造分类服务。
     *
     * @param categoryRepository       分类仓储
     * @param dishRepository           菜品仓储（删除前校验引用）
     * @param redisJsonCache           缓存
     * @param redisAppProperties       Redis 配置
     * @param kitchenCacheEvictService 失效服务
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public CategoryApplicationService(CategoryRepository categoryRepository,
                                      DishRepository dishRepository,
                                      RedisJsonCache redisJsonCache,
                                      RedisAppProperties redisAppProperties,
                                      KitchenCacheEvictService kitchenCacheEvictService) {
        this.categoryRepository = categoryRepository;
        this.dishRepository = dishRepository;
        this.redisJsonCache = redisJsonCache;
        this.redisAppProperties = redisAppProperties;
        this.kitchenCacheEvictService = kitchenCacheEvictService;
    }

    /**
     * 用户端：启用中的分类列表（Redis 缓存）。
     *
     * @return 分类 VO 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public List<CategoryVo> listEnabled() {
        return redisJsonCache.getOrLoad(
                CacheKeys.categoriesEnabled(),
                CATEGORY_LIST_TYPE,
                redisAppProperties.getCache().getCategoryTtlSeconds(),
                () -> categoryRepository.findAllEnabled().stream().map(this::toVo).toList()
        );
    }

    /**
     * 管理端：全部分类（含停用）。
     *
     * @return 分类 VO 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public List<CategoryVo> listAll() {
        return categoryRepository.findAll().stream().map(this::toVo).toList();
    }

    /**
     * 管理端：创建分类。
     *
     * @param dto 请求
     * @return 新建分类
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public CategoryVo create(CategorySaveDto dto) {
        Category category = new Category();
        applyDto(category, dto, true);
        CategoryVo vo = toVo(insert(categoryRepository, category));
        kitchenCacheEvictService.evictCategories();
        return vo;
    }

    /**
     * 管理端：更新分类（含排序、启停）。
     *
     * @param id  分类 ID
     * @param dto 请求
     * @return 更新后分类
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public CategoryVo update(Long id, CategorySaveDto dto) {
        Category category = requireById(categoryRepository, id, "分类不存在");
        applyDto(category, dto, false);
        CategoryVo vo = toVo(update(categoryRepository, category));
        kitchenCacheEvictService.evictCategories();
        return vo;
    }

    /**
     * 管理端：物理删除分类；仍有菜品时拒绝。
     *
     * @param id 分类 ID
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        requireById(categoryRepository, id, "分类不存在");
        if (dishRepository.countByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下仍有菜品，无法删除");
        }
        deleteById(categoryRepository, id);
        kitchenCacheEvictService.evictCategories();
    }

    /**
     * 将 DTO 应用到领域对象。
     *
     * @param category 领域对象
     * @param dto      请求
     * @param creating 是否新建
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
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
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    private CategoryVo toVo(Category category) {
        return new CategoryVo()
                .setId(category.getId())
                .setName(category.getName())
                .setIcon(category.getIcon())
                .setSortOrder(category.getSortOrder())
                .setStatus(category.getStatus())
                .setUpdatedAt(category.getUpdatedAt());
    }
}

