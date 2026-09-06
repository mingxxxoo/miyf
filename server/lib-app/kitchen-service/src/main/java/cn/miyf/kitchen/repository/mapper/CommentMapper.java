package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.CommentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 评论 Mapper；聚合与分页见 CommentMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {

    /**
     * 是否已存在评价。
     *
     * @param orderId 预约 ID
     * @param dishId  菜品 ID
     * @param userId  用户 ID
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countExists(@Param("orderId") Long orderId,
                     @Param("dishId") Long dishId,
                     @Param("userId") Long userId);

    /**
     * 更新评论状态。
     *
     * @param id     评论 ID
     * @param status 状态
     * @return 影响行数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 菜品评价分页。
     *
     * @param dishId 菜品 ID
     * @param rating 星级
     * @param sort   排序
     * @param offset 偏移
     * @param limit  条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CommentEntity> selectByDishPage(@Param("dishId") Long dishId,
                                         @Param("rating") Integer rating,
                                         @Param("sort") String sort,
                                         @Param("offset") long offset,
                                         @Param("limit") long limit);

    /**
     * 菜品评价分页总数。
     *
     * @param dishId 菜品 ID
     * @param rating 星级
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countByDishPage(@Param("dishId") Long dishId, @Param("rating") Integer rating);

    /**
     * 管理端分页。
     *
     * @param dishId 菜品 ID
     * @param status 状态
     * @param rating 星级
     * @param offset 偏移
     * @param limit  条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CommentEntity> selectAdminPage(@Param("dishId") Long dishId,
                                        @Param("status") String status,
                                        @Param("rating") Integer rating,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 管理端分页总数。
     *
     * @param dishId 菜品 ID
     * @param status 状态
     * @param rating 星级
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countAdminPage(@Param("dishId") Long dishId,
                        @Param("status") String status,
                        @Param("rating") Integer rating);

    /**
     * 用户评价分页。
     *
     * @param userId 用户 ID
     * @param offset 偏移
     * @param limit  条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CommentEntity> selectByUserPage(@Param("userId") Long userId,
                                         @Param("offset") long offset,
                                         @Param("limit") long limit);

    /**
     * 用户评价分页总数。
     *
     * @param userId 用户 ID
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countByUserPage(@Param("userId") Long userId);

    /**
     * 聚合有效评价。
     *
     * @param dishId 菜品 ID
     * @return avg_rating / cnt
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    Map<String, Object> avgRating(@Param("dishId") Long dishId);

    /**
     * 有效评价总数。
     *
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countNormal();

    /**
     * 隐藏评价总数。
     *
     * @return 数量
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    long countHidden();

    /**
     * 有效评价按星级分组。
     *
     * @return 每行含 rating、cnt
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<Map<String, Object>> countGroupByRating();
}
