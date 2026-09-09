package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.RecipeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜谱数据访问接口（MyBatis Mapper）。
 * 单表 CRUD 继承 {@link BaseMapper}；按菜品查询等见 RecipeRepository.xml。
 * 业务约束「一菜一谱」由 Service 校验，本接口仅负责持久化。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface RecipeRepository extends BaseMapper<RecipeEntity> {

    /**
     * 按菜品 ID 查询菜谱（一菜一谱场景下至多一条）。
     *
     * @param dishId 菜品 ID
     * @return 菜谱实体，不存在时为 null
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    RecipeEntity selectByDishId(@Param("dishId") Long dishId);

    /**
     * 查询全部菜谱，供管理端列表。
     *
     * @return 菜谱列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<RecipeEntity> selectAll();
}
