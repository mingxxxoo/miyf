package cn.miyf.kitchen.service;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.QueryConditionHolder;
import cn.miyf.infrastructure.redis.RedisDistributedLock;
import cn.miyf.kitchen.bean.dto.CommentCreateDto;
import cn.miyf.kitchen.bean.entity.CommentEntity;
import cn.miyf.kitchen.bean.entity.CommentImageEntity;
import cn.miyf.kitchen.bean.entity.OrderEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Comment;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.bean.model.OrderStatus;
import cn.miyf.kitchen.bean.model.RatingAgg;
import cn.miyf.kitchen.bean.qo.CommentPageQo;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.bean.vo.DishRatingVo;
import cn.miyf.kitchen.constant.KitchenCacheKeys;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.CommentImageRepository;
import cn.miyf.kitchen.repository.CommentRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OrderItemRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价应用服务：完成单资格校验、隐藏/恢复/删除后评分重算。
 * 用户昵称/头像与图片 enrich、图集替换由本服务编排，Repository 仅返回 Entity。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Service
@RequiredArgsConstructor
public class CommentApplicationService extends BaseApplicationService {

    private final CommentRepository commentRepository;
    private final CommentImageRepository commentImageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;
    private final RedisDistributedLock redisDistributedLock;
    private final KitchenCacheEvictService kitchenCacheEvictService;

    /**
     * 用户发表评价。
     *
     * @param dto 请求
     * @return 评价 VO
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public CommentVo create(CommentCreateDto dto) {
        Long userId = SecurityUtils.currentUserId();
        int rating = dto.getRating();
        requireTrue(rating >= 1 && rating <= 5, ErrorCode.BAD_REQUEST, "评分仅允许 1~5 星");

        Order order = loadOrder(dto.getOrderId());
        requireTrue(order.getUserId().equals(userId), ErrorCode.FORBIDDEN, "只能评价自己的预约");
        requireTrue(OrderStatus.COMPLETED.name().equals(order.getStatus()),
                ErrorCode.ORDER_NOT_COMMENTABLE, "完成预约后才能评价");
        requireTrue(containsDish(order, dto.getDishId()),
                ErrorCode.ORDER_NOT_COMMENTABLE, "该预约不包含此菜品");
        requireTrue(commentRepository.countExists(dto.getOrderId(), dto.getDishId(), userId) == 0,
                ErrorCode.DUPLICATE_COMMENT);

        requireTrue(existsById(dishRepository, dto.getDishId()), ErrorCode.NOT_FOUND, "菜品不存在");

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setOrderId(dto.getOrderId());
        comment.setDishId(dto.getDishId());
        comment.setRating(rating);
        comment.setContent(dto.getContent());
        comment.setStatus("NORMAL");
        comment.setImages(dto.getImages());

        Comment saved;
        try {
            saved = persistComment(comment);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_COMMENT);
        }
        rebuildDishRating(dto.getDishId());
        return toVo(saved);
    }

    /**
     * 菜品公开评价分页（仅 NORMAL）。
     *
     * @param dishId 菜品 ID
     * @param qo     查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public PageResult<CommentVo> pageByDish(Long dishId, CommentPageQo qo) {
        requireTrue(existsById(dishRepository, dishId), ErrorCode.NOT_FOUND, "菜品不存在");
        if (qo.getRating() != null) {
            requireTrue(qo.getRating() >= 1 && qo.getRating() <= 5, ErrorCode.BAD_REQUEST, "星级筛选无效");
        }
        String sortMode = normalizeSortMode(qo.getSortMode());
        long page = pageOf(qo);
        long pageSize = pageSizeOf(qo);
        long off = offset(page, pageSize);
        List<CommentVo> records = commentRepository.selectByDishPage(dishId, qo.getRating(), sortMode, off, pageSize)
                .stream().map(e -> toVo(enrich(EntityConverters.toComment(e)))).toList();
        long total = commentRepository.countByDishPage(dishId, qo.getRating());
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * 管理端评价分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public PageResult<CommentVo> pageAdmin(CommentPageQo qo) {
        if (qo.getStatus() != null && !qo.getStatus().isBlank()) {
            String status = qo.getStatus().trim().toUpperCase(Locale.ROOT);
            requireTrue("NORMAL".equals(status) || "HIDDEN".equals(status),
                    ErrorCode.BAD_REQUEST, "评价状态无效");
            qo.setStatus(status);
        }
        if (qo.getRating() != null) {
            requireTrue(qo.getRating() >= 1 && qo.getRating() <= 5, ErrorCode.BAD_REQUEST, "星级筛选无效");
        }
        return QueryConditionHolder.run(qo, "create_time DESC", () -> {
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            long off = offset(page, pageSize);
            List<CommentVo> records = commentRepository.selectAdminPage(
                            qo.getDishId(), qo.getStatus(), qo.getRating(), off, pageSize)
                    .stream().map(e -> toVo(enrich(EntityConverters.toComment(e)))).toList();
            long total = commentRepository.countAdminPage(qo.getDishId(), qo.getStatus(), qo.getRating());
            return PageResult.of(records, total, page, pageSize);
        });
    }

    /**
     * 隐藏评价并重算评分。
     *
     * @param id 评价 ID
     * @return 评价
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public CommentVo hide(Long id) {
        Comment comment = requireComment(id);
        requireTrue(!"HIDDEN".equals(comment.getStatus()), ErrorCode.INVALID_STATUS, "评价已隐藏");
        commentRepository.updateStatus(id, "HIDDEN");
        rebuildDishRating(comment.getDishId());
        return toVo(requireComment(id));
    }

    /**
     * 恢复评价并重算评分。
     *
     * @param id 评价 ID
     * @return 评价
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public CommentVo restore(Long id) {
        Comment comment = requireComment(id);
        requireTrue(!"NORMAL".equals(comment.getStatus()), ErrorCode.INVALID_STATUS, "评价已是可见状态");
        commentRepository.updateStatus(id, "NORMAL");
        rebuildDishRating(comment.getDishId());
        return toVo(requireComment(id));
    }

    /**
     * 物理删除评价并重算评分。
     *
     * @param id 评价 ID
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        Comment comment = requireComment(id);
        Long dishId = comment.getDishId();
        commentImageRepository.deleteByCommentId(id);
        deleteById(commentRepository, id);
        rebuildDishRating(dishId);
    }

    /**
     * 按有效评论重算菜品评分字段（分布式锁防并发覆盖）。
     *
     * @param dishId 菜品 ID
     * @return 评分结果
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     * @history 1.01 2026-09-09 XieMingJie 暂时去掉 ES 同步，仅 SQL + 缓存失效.
     */
    @Transactional
    public DishRatingVo rebuildDishRating(Long dishId) {
        return redisDistributedLock.executeWithLock(KitchenCacheKeys.lockRating(String.valueOf(dishId)), () -> {
            requireTrue(existsById(dishRepository, dishId), ErrorCode.NOT_FOUND, "菜品不存在");
            RatingAgg agg = aggregateNormalByDish(dishId);
            dishRepository.updateRating(dishId, agg.avg(), agg.count());
            kitchenCacheEvictService.evictDishBrowse();
            return new DishRatingVo()
                    .setDishId(dishId)
                    .setRating(agg.avg())
                    .setRatingCount(agg.count());
        });
    }

    /**
     * 聚合有效评价的平均分与数量；驱动菜品 rating 回写。
     * JDBC 可能返回 BigDecimal 或其它 Number，需兼容解析。
     *
     * @param dishId 菜品 ID
     * @return 聚合结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private RatingAgg aggregateNormalByDish(Long dishId) {
        Map<String, Object> row = commentRepository.avgRating(dishId);
        if (row == null || row.isEmpty()) {
            return new RatingAgg(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
        }
        Object avgObj = row.get("avg_rating");
        Object cntObj = row.get("cnt");
        BigDecimal avg = BigDecimal.ZERO;
        if (avgObj instanceof BigDecimal decimal) {
            avg = decimal;
        } else if (avgObj instanceof Number number) {
            avg = BigDecimal.valueOf(number.doubleValue());
        }
        int count = cntObj instanceof Number number ? number.intValue() : 0;
        if (count <= 0) {
            return new RatingAgg(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
        }
        return new RatingAgg(avg.setScale(2, RoundingMode.HALF_UP), count);
    }

    /**
     * 加载预约并装载明细，用于评价资格校验。
     *
     * @param orderId 预约 ID
     * @return 含明细的预约
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Order loadOrder(Long orderId) {
        OrderEntity entity = requireById(orderRepository, orderId, "预约不存在");
        Order order = EntityConverters.toOrder(entity, null);
        order.setItems(orderItemRepository.selectByOrderId(orderId).stream()
                .map(EntityConverters::toOrderItem).toList());
        return order;
    }

    /**
     * 按 ID 加载评价并 enrich，不存在则抛出。
     *
     * @param id 评价 ID
     * @return 评价
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Comment requireComment(Long id) {
        return enrich(EntityConverters.toComment(requireById(commentRepository, id, "评价不存在")));
    }

    /**
     * 保存评价主表；images 非 null 时同步替换图集。
     *
     * @param comment 评价
     * @return 丰富后的评价
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Comment persistComment(Comment comment) {
        CommentEntity entity = EntityConverters.toCommentEntity(comment);
        save(commentRepository, entity);
        comment.setId(entity.getId());
        if (comment.getImages() != null) {
            replaceImages(entity.getId(), comment.getImages());
        }
        return enrich(EntityConverters.toComment(commentRepository.selectById(entity.getId())));
    }

    /**
     * 替换评价图片：先删后按顺序插入。
     *
     * @param commentId 评价 ID
     * @param imageUrls 图片 URL；空列表表示清空
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private void replaceImages(Long commentId, List<String> imageUrls) {
        commentImageRepository.deleteByCommentId(commentId);
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        int sort = 0;
        Instant now = Instant.now();
        for (String url : imageUrls) {
            CommentImageEntity img = new CommentImageEntity();
            img.setCommentId(commentId);
            img.setUrl(url);
            img.setSortOrder(sort++);
            img.setCreateTime(now);
            commentImageRepository.insert(img);
        }
    }

    /**
     * 补充用户昵称/头像与图片列表。
     *
     * @param comment 评价
     * @return 丰富后的评价
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private Comment enrich(Comment comment) {
        if (comment == null) {
            return null;
        }
        if (comment.getUserId() != null) {
            UserEntity user = userRepository.selectById(comment.getUserId());
            if (user != null) {
                comment.setUserNickname(user.getNickname());
                comment.setUserAvatar(user.getAvatarUrl());
            }
        }
        List<CommentImageEntity> images = commentImageRepository.selectByCommentId(comment.getId());
        comment.setImages(images.stream().map(CommentImageEntity::getUrl).collect(Collectors.toCollection(ArrayList::new)));
        return comment;
    }

    private boolean containsDish(Order order, Long dishId) {
        if (order.getItems() == null) {
            return false;
        }
        for (OrderItem item : order.getItems()) {
            if (dishId.equals(item.getDishId())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSortMode(String sortMode) {
        if (sortMode == null || sortMode.isBlank()) {
            return "latest";
        }
        String mode = sortMode.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "latest", "rating_asc", "rating_desc" -> mode;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "排序方式无效");
        };
    }

    private CommentVo toVo(Comment comment) {
        return new CommentVo()
                .setId(comment.getId())
                .setUserId(comment.getUserId())
                .setUserNickname(comment.getUserNickname())
                .setUserAvatar(comment.getUserAvatar())
                .setDishId(comment.getDishId())
                .setOrderId(comment.getOrderId())
                .setRating(comment.getRating())
                .setContent(comment.getContent())
                .setStatus(comment.getStatus())
                .setImages(comment.getImages())
                .setCreateTime(comment.getCreateTime());
    }
}
