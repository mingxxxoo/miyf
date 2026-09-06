package cn.miyf.kitchen.repository.mapper;

import cn.miyf.kitchen.bean.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper；复杂查询见 UserMapper.xml。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 按 openid 查询。
     *
     * @param openid 微信 openid
     * @return 用户实体
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    UserEntity selectByOpenid(@Param("openid") String openid);

    /**
     * 管理端分页。
     *
     * @param keyword 关键词
     * @param status  状态
     * @param offset  偏移
     * @param limit   条数
     * @return 列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<UserEntity> selectAdminPage(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /**
     * 管理端分页总数。
     *
     * @param keyword 关键词
     * @param status  状态
     * @return 总数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countAdminPage(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 更新状态。
     *
     * @param id     用户 ID
     * @param status 状态
     * @return 影响行数
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计启用用户。
     *
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countActive();
}
