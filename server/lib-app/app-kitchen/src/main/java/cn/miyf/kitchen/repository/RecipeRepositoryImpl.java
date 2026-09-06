package cn.miyf.kitchen.repository;

import cn.miyf.repository.AbstractMybatisRepository;

import cn.miyf.kitchen.bean.model.Recipe;
import cn.miyf.kitchen.repository.RecipeRepository;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.repository.mapper.RecipeMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 菜谱仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Repository
public class RecipeRepositoryImpl extends AbstractMybatisRepository<Recipe, RecipeEntity>
        implements RecipeRepository {

    private final RecipeMapper recipeMapper;

    /**
     * 构造菜谱仓储。
     *
     * @param recipeMapper 菜谱 Mapper
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    public RecipeRepositoryImpl(RecipeMapper recipeMapper) {
        super(recipeMapper, EntityConverters::toRecipe, EntityConverters::toRecipeEntity);
        this.recipeMapper = recipeMapper;
    }

    @Override
    protected Long getDomainId(Recipe domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(Recipe domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(Recipe domain, Instant createdAt, Instant updatedAt) {
        domain.setCreatedAt(createdAt);
        domain.setUpdatedAt(updatedAt);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Override
    public Optional<Recipe> findByDishId(Long dishId) {
        return Optional.ofNullable(EntityConverters.toRecipe(recipeMapper.selectByDishId(dishId)));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Override
    public List<Recipe> findAll() {
        return recipeMapper.selectAll().stream().map(EntityConverters::toRecipe).toList();
    }
}
