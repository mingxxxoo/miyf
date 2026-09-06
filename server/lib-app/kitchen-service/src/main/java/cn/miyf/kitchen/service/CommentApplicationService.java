package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.QueryConditionHolder;
import cn.miyf.infrastructure.redis.RedisDistributedLock;
import cn.miyf.kitchen.bean.dto.CommentCreateDto;
import cn.miyf.kitchen.bean.model.Comment;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.bean.model.OrderStatus;
import cn.miyf.kitchen.bean.qo.CommentPageQo;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.bean.vo.DishRatingVo;
import cn.miyf.kitchen.constant.CacheKeys;
import cn.miyf.kitchen.repository.CommentRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.security.SecurityUtils;
import cn.miyf.service.BaseApplicationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 评价应用服务：完成单资格校验、隐藏/恢复/删除后评分重算。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Service
public class CommentApplicationService extends BaseApplicationService {

    private final CommentRepository commentRepository;
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final RedisDistributedLock redisDistributedLock;
    private final KitchenCacheEvictService kitchenCacheEvictService;

    /**
     * 构造评价服务。
     *
     * @param commentRepository        评价仓储
     * @param orderRepository          预约仓储
     * @param dishRepository           菜品仓储
     * @param redisDistributedLock     分布式锁
     * @param kitchenCacheEvictService 缓存失效
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public CommentApplicationService(CommentRepository commentRepository,
                                     OrderRepository orderRepository,
                                     DishRepository dishRepository,
                                     RedisDistributedLock redisDistributedLock,
                                     KitchenCacheEvictService kitchenCacheEvictService) {
        this.commentRepository = commentRepository;
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.redisDistributedLock = redisDistributedLock;
        this.kitchenCacheEvictService = kitchenCacheEvictService;
    }

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

        Order order = requireById(orderRepository, dto.getOrderId(), "预约不存在");
        requireTrue(order.getUserId().equals(userId), ErrorCode.FORBIDDEN, "只能评价自己的预约");
        requireTrue(OrderStatus.COMPLETED.name().equals(order.getStatus()),
                ErrorCode.ORDER_NOT_COMMENTABLE, "完成预约后才能评价");
        requireTrue(containsDish(order, dto.getDishId()),
                ErrorCode.ORDER_NOT_COMMENTABLE, "该预约不包含此菜品");
        requireTrue(!commentRepository.exists(dto.getOrderId(), dto.getDishId(), userId),
                ErrorCode.DUPLICATE_COMMENT);

        requireTrue(dishRepository.existsById(dto.getDishId()), ErrorCode.NOT_FOUND, "菜品不存在");

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
            saved = save(commentRepository, comment);
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
        requireTrue(dishRepository.existsById(dishId), ErrorCode.NOT_FOUND, "菜品不存在");
        if (qo.getRating() != null) {
            requireTrue(qo.getRating() >= 1 && qo.getRating() <= 5, ErrorCode.BAD_REQUEST, "星级筛选无效");
        }
        String sortMode = normalizeSortMode(qo.getSortMode());
        long page = pageOf(qo);
        long pageSize = pageSizeOf(qo);
        PageResult<Comment> result = commentRepository.pageByDish(dishId, qo.getRating(), sortMode, page, pageSize);
        return PageResult.of(result.records().stream().map(this::toVo).toList(),
                result.total(), result.page(), result.pageSize());
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
        return QueryConditionHolder.run(qo, "created_at DESC", () -> {
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            PageResult<Comment> result = commentRepository.pageAdmin(
                    qo.getDishId(), qo.getStatus(), qo.getRating(), page, pageSize);
            return PageResult.of(result.records().stream().map(this::toVo).toList(),
                    result.total(), result.page(), result.pageSize());
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
        Comment comment = requireById(commentRepository, id, "评价不存在");
        requireTrue(!"HIDDEN".equals(comment.getStatus()), ErrorCode.INVALID_STATUS, "评价已隐藏");
        commentRepository.updateStatus(id, "HIDDEN");
        rebuildDishRating(comment.getDishId());
        return toVo(requireById(commentRepository, id, "评价不存在"));
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
        Comment comment = requireById(commentRepository, id, "评价不存在");
        requireTrue(!"NORMAL".equals(comment.getStatus()), ErrorCode.INVALID_STATUS, "评价已是可见状态");
        commentRepository.updateStatus(id, "NORMAL");
        rebuildDishRating(comment.getDishId());
        return toVo(requireById(commentRepository, id, "评价不存在"));
    }

    /**
     * 物理删除评价并重算评分。
     *
     * @param id 评价 ID
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        Comment comment = requireById(commentRepository, id, "评价不存在");
        Long dishId = comment.getDishId();
        deleteById(commentRepository, id);
        rebuildDishRating(dishId);
    }

    /**
     * 按有效评论重算菜品评分字段（分布式锁防并发覆盖）。
     *
     * @param dishId 菜品 ID
     * @return 评分结果
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Transactional
    public DishRatingVo rebuildDishRating(Long dishId) {
        return redisDistributedLock.executeWithLock(CacheKeys.lockRating(String.valueOf(dishId)), () -> {
            requireTrue(dishRepository.existsById(dishId), ErrorCode.NOT_FOUND, "菜品不存在");
            CommentRepository.RatingAgg agg = commentRepository.aggregateNormalByDish(dishId);
            dishRepository.updateRating(dishId, agg.avg(), agg.count());
            kitchenCacheEvictService.evictDishBrowse();
            return new DishRatingVo()
                    .setDishId(dishId)
                    .setRating(agg.avg())
                    .setRatingCount(agg.count());
        });
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
                .setCreatedAt(comment.getCreatedAt());
    }
}
