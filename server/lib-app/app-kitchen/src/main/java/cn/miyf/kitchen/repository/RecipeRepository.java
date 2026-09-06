package cn.miyf.kitchen.repository;

import cn.miyf.repository.BaseRepository;

import cn.miyf.kitchen.bean.model.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * 菜谱仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface RecipeRepository extends BaseRepository<Recipe, Long> {

    /**
     * 按菜品 ID 查询菜谱（一菜一谱）。
     *
     * @param dishId 菜品 ID
     * @return 菜谱
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    Optional<Recipe> findByDishId(Long dishId);

    /**
     * 查询全部菜谱。
     *
     * @return 菜谱列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<Recipe> findAll();
}
