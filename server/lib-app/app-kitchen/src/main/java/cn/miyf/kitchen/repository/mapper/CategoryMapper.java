package cn.miyf.kitchen.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.miyf.kitchen.bean.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 分类 Mapper；列表查询见 CategoryMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {

    /**
     * 查询启用分类，按 sort_order 升序。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CategoryEntity> selectAllEnabled();

    /**
     * 查询全部分类，按 sort_order 升序。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CategoryEntity> selectAllOrdered();
}
