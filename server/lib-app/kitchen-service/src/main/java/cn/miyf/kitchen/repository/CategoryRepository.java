package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.model.Category;
import cn.miyf.repository.BaseRepository;

import java.util.List;

/**
 * 菜品分类仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface CategoryRepository extends BaseRepository<Category, Long> {

    /**
     * 查询全部启用分类（用户端）。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<Category> findAllEnabled();

    /**
     * 查询全部分类（管理端）。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<Category> findAll();
}
