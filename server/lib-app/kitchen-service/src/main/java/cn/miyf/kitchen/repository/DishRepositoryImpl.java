package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.DishImageEntity;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.mapper.CategoryMapper;
import cn.miyf.kitchen.repository.mapper.DishImageMapper;
import cn.miyf.kitchen.repository.mapper.DishMapper;
import cn.miyf.repository.AbstractMybatisRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 菜品仓储实现。
 * <p>
 * 单表 CRUD 走基类；分页/热门/库存/评分走 Mapper XML。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@Repository
public class DishRepositoryImpl extends AbstractMybatisRepository<Dish, DishEntity> implements DishRepository {

    private final DishMapper dishMapper;
    private final DishImageMapper dishImageMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 构造菜品仓储。
     *
     * @param dishMapper      菜品 Mapper
     * @param dishImageMapper 菜品图片 Mapper
     * @param categoryMapper  分类 Mapper（补充分类名）
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public DishRepositoryImpl(DishMapper dishMapper,
                              DishImageMapper dishImageMapper,
                              CategoryMapper categoryMapper) {
        super(dishMapper, EntityConverters::toDish, EntityConverters::toDishEntity);
        this.dishMapper = dishMapper;
        this.dishImageMapper = dishImageMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    protected Long getDomainId(Dish domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(Dish domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(Dish domain, Instant createTime, Instant lastModifyTime) {
        domain.setCreateTime(createTime);
        domain.setLastModifyTime(lastModifyTime);
    }

    /**
     * 插入前补齐评分默认值，避免空指针。
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    protected void beforeInsert(DishEntity entity, Dish domain) {
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
    }

    /**
     * {@inheritDoc}
     * <p>
     * 查询后补充分类名与图集。
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public Optional<Dish> findById(Long id) {
        return super.findById(id).map(this::enrich);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 保存后若携带 images，则替换图集。
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public Dish save(Dish dish) {
        Dish saved = super.save(dish);
        if (dish.getImages() != null) {
            replaceImages(saved.getId(), dish.getImages());
        }
        return findById(saved.getId()).orElse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public PageResult<Dish> pageUser(Long categoryId, String keyword, Boolean recommend, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Dish> records = dishMapper.selectUserPage(categoryId, keyword, recommend, offset, pageSize)
                .stream().map(e -> enrich(EntityConverters.toDish(e))).toList();
        long total = dishMapper.countUserPage(categoryId, keyword, recommend);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public PageResult<Dish> pageAdmin(Long categoryId, String status, String keyword, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Dish> records = dishMapper.selectAdminPage(categoryId, status, keyword, offset, pageSize)
                .stream().map(e -> enrich(EntityConverters.toDish(e))).toList();
        long total = dishMapper.countAdminPage(categoryId, status, keyword);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public List<Dish> findHot(int limit) {
        return dishMapper.selectHot(limit).stream().map(e -> enrich(EntityConverters.toDish(e))).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public List<Dish> findRecommend(int limit) {
        return dishMapper.selectRecommend(limit).stream().map(e -> enrich(EntityConverters.toDish(e))).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public boolean deductStock(Long dishId, int quantity) {
        // XML 中 WHERE stock >= qty，0 行即并发下库存不足
        return dishMapper.deductStock(dishId, quantity) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public void restoreStock(Long dishId, int quantity) {
        dishMapper.restoreStock(dishId, quantity);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public void updateRating(Long dishId, BigDecimal rating, int ratingCount) {
        dishMapper.updateRating(dishId, rating, ratingCount);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public void replaceImages(Long dishId, List<String> imageUrls) {
        // 图集为附属数据：先清空再按顺序写入，保证排序稳定
        dishImageMapper.deleteByDishId(dishId);
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String url : imageUrls) {
            DishImageEntity img = new DishImageEntity();
            img.setDishId(dishId);
            img.setUrl(url);
            img.setSortOrder(sort++);
            img.setCreateTime(Instant.now());
            dishImageMapper.insert(img);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public long countOnSale() {
        return dishMapper.countOnSale();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public boolean hasOrderHistory(Long dishId) {
        return dishMapper.countOrderItems(dishId) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Override
    public long countByCategoryId(Long categoryId) {
        return dishMapper.countByCategoryId(categoryId);
    }

    /**
     * 补充分类名与图片列表，供展示层直接使用。
     *
     * @param dish 菜品
     * @return 丰富后的菜品
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    private Dish enrich(Dish dish) {
        if (dish == null) {
            return null;
        }
        if (dish.getCategoryId() != null) {
            CategoryEntity category = categoryMapper.selectById(dish.getCategoryId());
            if (category != null) {
                dish.setCategoryName(category.getName());
            }
        }
        List<DishImageEntity> images = dishImageMapper.selectByDishId(dish.getId());
        dish.setImages(images.stream().map(DishImageEntity::getUrl).collect(Collectors.toCollection(ArrayList::new)));
        return dish;
    }
}
