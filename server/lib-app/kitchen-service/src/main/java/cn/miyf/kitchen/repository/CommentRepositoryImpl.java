package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.entity.CommentEntity;
import cn.miyf.kitchen.bean.entity.CommentImageEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Comment;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.mapper.CommentImageMapper;
import cn.miyf.kitchen.repository.mapper.CommentMapper;
import cn.miyf.kitchen.repository.mapper.UserMapper;
import cn.miyf.repository.AbstractMybatisRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 评价仓储实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Repository
public class CommentRepositoryImpl extends AbstractMybatisRepository<Comment, CommentEntity>
        implements CommentRepository {

    private final CommentMapper commentMapper;
    private final CommentImageMapper commentImageMapper;
    private final UserMapper userMapper;

    /**
     * 构造评价仓储。
     *
     * @param commentMapper      评论 Mapper
     * @param commentImageMapper 图片 Mapper
     * @param userMapper         用户 Mapper（补昵称头像）
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public CommentRepositoryImpl(CommentMapper commentMapper,
                                 CommentImageMapper commentImageMapper,
                                 UserMapper userMapper) {
        super(commentMapper, EntityConverters::toComment, EntityConverters::toCommentEntity);
        this.commentMapper = commentMapper;
        this.commentImageMapper = commentImageMapper;
        this.userMapper = userMapper;
    }

    @Override
    protected Long getDomainId(Comment domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(Comment domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(Comment domain, Instant createTime, Instant lastModifyTime) {
        domain.setCreateTime(createTime);
        domain.setLastModifyTime(lastModifyTime);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public Optional<Comment> findById(Long id) {
        return super.findById(id).map(this::enrich);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public Comment save(Comment comment) {
        Comment saved = super.save(comment);
        if (comment.getImages() != null) {
            replaceImages(saved.getId(), comment.getImages());
        }
        return findById(saved.getId()).orElse(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 先清理评论图片，再 {@code deleteBatchIds} 物理删除评论。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> idList = ids.stream().filter(id -> id != null).distinct().toList();
        if (idList.isEmpty()) {
            return;
        }
        for (Long id : idList) {
            commentImageMapper.deleteByCommentId(id);
        }
        commentMapper.deleteBatchIds(idList);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public void deleteById(Long id) {
        commentImageMapper.deleteByCommentId(id);
        super.deleteById(id);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public void updateStatus(Long id, String status) {
        commentMapper.updateStatus(id, status);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public boolean exists(Long orderId, Long dishId, Long userId) {
        return commentMapper.countExists(orderId, dishId, userId) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public PageResult<Comment> pageByDish(Long dishId, Integer rating, String sort, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Comment> records = commentMapper.selectByDishPage(dishId, rating, sort, offset, pageSize).stream()
                .map(e -> enrich(EntityConverters.toComment(e)))
                .toList();
        long total = commentMapper.countByDishPage(dishId, rating);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public PageResult<Comment> pageAdmin(Long dishId, String status, Integer rating, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Comment> records = commentMapper.selectAdminPage(dishId, status, rating, offset, pageSize).stream()
                .map(e -> enrich(EntityConverters.toComment(e)))
                .toList();
        long total = commentMapper.countAdminPage(dishId, status, rating);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public PageResult<Comment> pageByUser(Long userId, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Comment> records = commentMapper.selectByUserPage(userId, offset, pageSize).stream()
                .map(e -> enrich(EntityConverters.toComment(e)))
                .toList();
        long total = commentMapper.countByUserPage(userId);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public RatingAgg aggregateNormalByDish(Long dishId) {
        Map<String, Object> row = commentMapper.avgRating(dishId);
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
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public long countNormal() {
        return commentMapper.countNormal();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public long countHidden() {
        return commentMapper.countHidden();
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<RatingCount> ratingDistribution() {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : commentMapper.countGroupByRating()) {
            Object rating = row.get("rating");
            Object cnt = row.get("cnt");
            if (rating instanceof Number r && cnt instanceof Number c) {
                map.put(r.intValue(), c.longValue());
            }
        }
        List<RatingCount> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            list.add(new RatingCount(i, map.getOrDefault(i, 0L)));
        }
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Override
    public void replaceImages(Long commentId, List<String> imageUrls) {
        commentImageMapper.deleteByCommentId(commentId);
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
            commentImageMapper.insert(img);
        }
    }

    private Comment enrich(Comment comment) {
        if (comment == null) {
            return null;
        }
        if (comment.getUserId() != null) {
            UserEntity user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                comment.setUserNickname(user.getNickname());
                comment.setUserAvatar(user.getAvatarUrl());
            }
        }
        List<CommentImageEntity> images = commentImageMapper.selectByCommentId(comment.getId());
        comment.setImages(images.stream().map(CommentImageEntity::getUrl).collect(Collectors.toCollection(ArrayList::new)));
        return comment;
    }
}
