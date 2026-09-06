package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.RecipeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜谱 Mapper；查询见 RecipeMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface RecipeMapper extends BaseMapper<RecipeEntity> {

    /**
     * 按菜品 ID 查询菜谱。
     *
     * @param dishId 菜品 ID
     * @return 菜谱实体
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    RecipeEntity selectByDishId(@Param("dishId") Long dishId);

    /**
     * 查询全部菜谱。
     *
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<RecipeEntity> selectAll();
}
