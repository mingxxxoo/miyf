package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.model.Category;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.mapper.CategoryMapper;
import cn.miyf.repository.AbstractMybatisRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 分类仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Repository
public class CategoryRepositoryImpl extends AbstractMybatisRepository<Category, CategoryEntity>
        implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    /**
     * 构造分类仓储。
     *
     * @param categoryMapper 分类 Mapper
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public CategoryRepositoryImpl(CategoryMapper categoryMapper) {
        super(categoryMapper, EntityConverters::toCategory, EntityConverters::toCategoryEntity);
        this.categoryMapper = categoryMapper;
    }

    @Override
    protected Long getDomainId(Category domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(Category domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(Category domain, Instant createTime, Instant lastModifyTime) {
        domain.setCreateTime(createTime);
        domain.setLastModifyTime(lastModifyTime);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public List<Category> findAllEnabled() {
        return categoryMapper.selectAllEnabled().stream().map(EntityConverters::toCategory).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    @Override
    public List<Category> findAll() {
        return categoryMapper.selectAllOrdered().stream().map(EntityConverters::toCategory).toList();
    }
}
