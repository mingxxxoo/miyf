package cn.miyf.kitchen.repository;

import cn.miyf.repository.BaseRepository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.Comment;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评价仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface CommentRepository extends BaseRepository<Comment, Long> {

    /**
     * 更新评论状态（NORMAL / HIDDEN）。
     *
     * @param id     评论 ID
     * @param status 状态
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    void updateStatus(Long id, String status);

    /**
     * 判断同一预约+菜品+用户是否已评价。
     *
     * @param orderId 预约 ID
     * @param dishId  菜品 ID
     * @param userId  用户 ID
     * @return true 已存在
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    boolean exists(Long orderId, Long dishId, Long userId);

    /**
     * 菜品维度评价分页（仅 NORMAL）。
     *
     * @param dishId   菜品 ID
     * @param rating   星级筛选，可空
     * @param sort     排序：latest / rating_asc / rating_desc
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<Comment> pageByDish(Long dishId, Integer rating, String sort, long page, long pageSize);

    /**
     * 管理端评价分页。
     *
     * @param dishId   菜品 ID，可空
     * @param status   状态，可空
     * @param rating   星级，可空
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<Comment> pageAdmin(Long dishId, String status, Integer rating, long page, long pageSize);

    /**
     * 用户本人评价分页。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<Comment> pageByUser(Long userId, long page, long pageSize);

    /**
     * 聚合有效评价的平均分与数量。
     *
     * @param dishId 菜品 ID
     * @return 聚合结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    RatingAgg aggregateNormalByDish(Long dishId);

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
     * 有效评价星级分布（1~5，缺失星级补 0）。
     *
     * @return rating → count
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<RatingCount> ratingDistribution();

    /**
     * 替换评论图片。
     *
     * @param commentId 评论 ID
     * @param imageUrls 图片 URL
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    void replaceImages(Long commentId, List<String> imageUrls);

    /**
     * 评分聚合值对象。
     *
     * @param avg   平均分
     * @param count 数量
     * @author XieMingJie
     * @since 2026-09-04 16:41
     */
    record RatingAgg(BigDecimal avg, int count) {}

    /**
     * 星级分布项。
     *
     * @param rating 星级
     * @param count  数量
     */
    record RatingCount(int rating, long count) {}
}
