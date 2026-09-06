package cn.miyf.kitchen.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.miyf.kitchen.bean.entity.DishEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品持久化 Mapper。
 * <p>
 * 单表 CRUD 继承 {@link BaseMapper}；分页/热门/库存/评分等复杂 SQL 见 DishMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@Mapper
public interface DishMapper extends BaseMapper<DishEntity> {

    /**
     * 用户端分页查询已上架菜品。
     *
     * @param categoryId 分类 ID，可空
     * @param keyword    关键词，可空
     * @param recommend  是否仅推荐，可空
     * @param offset     偏移量
     * @param limit      条数
     * @return 菜品实体列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<DishEntity> selectUserPage(@Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword,
                                    @Param("recommend") Boolean recommend,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    /**
     * 用户端分页总数。
     *
     * @param categoryId 分类 ID，可空
     * @param keyword    关键词，可空
     * @param recommend  是否仅推荐，可空
     * @return 总数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countUserPage(@Param("categoryId") Long categoryId,
                       @Param("keyword") String keyword,
                       @Param("recommend") Boolean recommend);

    /**
     * 管理端分页查询。
     *
     * @param categoryId 分类 ID，可空
     * @param status     状态，可空
     * @param keyword    关键词，可空
     * @param offset     偏移量
     * @param limit      条数
     * @return 菜品实体列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<DishEntity> selectAdminPage(@Param("categoryId") Long categoryId,
                                     @Param("status") String status,
                                     @Param("keyword") String keyword,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /**
     * 管理端分页总数。
     *
     * @param categoryId 分类 ID，可空
     * @param status     状态，可空
     * @param keyword    关键词，可空
     * @return 总数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countAdminPage(@Param("categoryId") Long categoryId,
                        @Param("status") String status,
                        @Param("keyword") String keyword);

    /**
     * 热门菜品：按评分、评价数降序。
     *
     * @param limit 条数
     * @return 菜品列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<DishEntity> selectHot(@Param("limit") int limit);

    /**
     * 今日推荐菜品。
     *
     * @param limit 条数
     * @return 菜品列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<DishEntity> selectRecommend(@Param("limit") int limit);

    /**
     * 有限量模式下原子扣减可提供份数。
     * <p>
     * 条件包含 stock &gt;= qty，用于并发安全。
     *
     * @param id  菜品 ID
     * @param qty 扣减数量
     * @return 影响行数，0 表示库存不足
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * 有限量模式下回补可提供份数（取消预约时）。
     *
     * @param id  菜品 ID
     * @param qty 回补数量
     * @return 影响行数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * 更新菜品聚合评分（仅由评论域触发，禁止管理端直接改）。
     *
     * @param id          菜品 ID
     * @param rating      平均分
     * @param ratingCount 有效评价数
     * @return 影响行数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    int updateRating(@Param("id") Long id,
                     @Param("rating") BigDecimal rating,
                     @Param("ratingCount") int ratingCount);

    /**
     * 统计菜品是否出现在历史预约明细中（决定只能逻辑删除）。
     *
     * @param dishId 菜品 ID
     * @return 明细条数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countOrderItems(@Param("dishId") Long dishId);

    /**
     * 统计上架菜品数量。
     *
     * @return 上架数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countOnSale();

    /**
     * 统计分类下未删除菜品数（删除分类前校验）。
     *
     * @param categoryId 分类 ID
     * @return 菜品数
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
