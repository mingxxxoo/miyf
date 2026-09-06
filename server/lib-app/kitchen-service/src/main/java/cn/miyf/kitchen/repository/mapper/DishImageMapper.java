package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.DishImageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品图片 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@Mapper
public interface DishImageMapper extends BaseMapper<DishImageEntity> {

    /**
     * 按菜品查询图片，按排序升序。
     *
     * @param dishId 菜品 ID
     * @return 图片列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<DishImageEntity> selectByDishId(@Param("dishId") Long dishId);

    /**
     * 按菜品删除全部图片。
     *
     * @param dishId 菜品 ID
     * @return 影响行数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    int deleteByDishId(@Param("dishId") Long dishId);
}
