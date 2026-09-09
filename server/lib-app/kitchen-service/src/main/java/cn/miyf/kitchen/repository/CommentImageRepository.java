package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.CommentImageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价图片数据访问接口（MyBatis Mapper）。
 * 删除评价前需先清理图片，避免孤儿记录。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface CommentImageRepository extends BaseMapper<CommentImageEntity> {

    /**
     * 按评价查询图片，按 sort_order 升序。
     *
     * @param commentId 评价 ID
     * @return 图片列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<CommentImageEntity> selectByCommentId(@Param("commentId") Long commentId);

    /**
     * 按评价删除全部图片，用于图集替换或删除评价前置清理。
     *
     * @param commentId 评价 ID
     * @return 影响行数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    int deleteByCommentId(@Param("commentId") Long commentId);
}
