package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.repository.BaseRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品领域仓储。
 * <p>
 * 继承 {@link BaseRepository} 获得单表 CRUD；分页/库存/评分等复杂能力在此扩展。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public interface DishRepository extends BaseRepository<Dish, Long> {

    /**
     * 用户端分页（仅 ON_SALE）。
     *
     * @param categoryId 分类，可空
     * @param keyword    关键词，可空
     * @param recommend  是否推荐，可空
     * @param page       页码
     * @param pageSize   每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    PageResult<Dish> pageUser(Long categoryId, String keyword, Boolean recommend, long page, long pageSize);

    /**
     * 管理端分页。
     *
     * @param categoryId 分类，可空
     * @param status     状态，可空
     * @param keyword    关键词，可空
     * @param page       页码
     * @param pageSize   每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    PageResult<Dish> pageAdmin(Long categoryId, String status, String keyword, long page, long pageSize);

    /**
     * 热门菜品列表。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<Dish> findHot(int limit);

    /**
     * 推荐菜品列表。
     *
     * @param limit 条数
     * @return 列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<Dish> findRecommend(int limit);

    /**
     * 有限量库存原子扣减。
     *
     * @param dishId   菜品 ID
     * @param quantity 数量
     * @return true 扣减成功
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    boolean deductStock(Long dishId, int quantity);

    /**
     * 有限量库存回补。
     *
     * @param dishId   菜品 ID
     * @param quantity 数量
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    void restoreStock(Long dishId, int quantity);

    /**
     * 写入聚合评分。
     *
     * @param dishId      菜品 ID
     * @param rating      平均分
     * @param ratingCount 评价数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    void updateRating(Long dishId, BigDecimal rating, int ratingCount);

    /**
     * 替换菜品图集（先删后插）。
     *
     * @param dishId    菜品 ID
     * @param imageUrls 图片 URL 列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    void replaceImages(Long dishId, List<String> imageUrls);

    /**
     * 上架菜品数量。
     *
     * @return 数量
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countOnSale();

    /**
     * 是否存在预约历史（决定删除策略）。
     *
     * @param dishId 菜品 ID
     * @return true 表示有历史
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    boolean hasOrderHistory(Long dishId);

    /**
     * 统计分类下未删除菜品数量。
     *
     * @param categoryId 分类 ID
     * @return 菜品数
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    long countByCategoryId(Long categoryId);
}
