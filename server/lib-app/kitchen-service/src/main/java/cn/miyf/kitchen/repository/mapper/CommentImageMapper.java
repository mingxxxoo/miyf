package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.CommentImageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论图片 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface CommentImageMapper extends BaseMapper<CommentImageEntity> {

    /**
     * 按评论查询图片。
     *
     * @param commentId 评论 ID
     * @return 图片列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<CommentImageEntity> selectByCommentId(@Param("commentId") Long commentId);

    /**
     * 按评论删除图片。
     *
     * @param commentId 评论 ID
     * @return 影响行数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    int deleteByCommentId(@Param("commentId") Long commentId);
}
