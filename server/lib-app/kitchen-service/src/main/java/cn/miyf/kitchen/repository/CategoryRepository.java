package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜品分类数据访问接口（MyBatis Mapper）。
 * 单表 CRUD 继承 {@link BaseMapper}；有序列表查询见 CategoryRepository.xml。
 * 合并原 CategoryMapper，去掉 RepositoryImpl。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface CategoryRepository extends BaseMapper<CategoryEntity> {

    /**
     * 查询启用分类，按 sort_order 升序，供用户端分类导航。
     *
     * @return 启用分类列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<CategoryEntity> selectAllEnabled();

    /**
     * 查询全部分类（含停用），按 sort_order 升序，供管理端维护。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<CategoryEntity> selectAllOrdered();
}
