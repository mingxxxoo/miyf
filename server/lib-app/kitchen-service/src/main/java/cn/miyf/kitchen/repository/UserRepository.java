package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 厨房用户数据访问接口（MyBatis Mapper）。
 * 单表 CRUD 继承 {@link BaseMapper}；按 openid / 管理端分页等复杂 SQL 见 UserRepository.xml。
 * 合并原 UserMapper，去掉 RepositoryImpl，由 Service 负责 Entity 与领域对象转换。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface UserRepository extends BaseMapper<UserEntity> {

    /**
     * 按微信 openid 查询用户，用于登录绑定与账号查找。
     *
     * @param openid 微信 openid
     * @return 用户实体，不存在时为 null
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    UserEntity selectByOpenid(@Param("openid") String openid);

    /**
     * 管理端用户分页列表，支持昵称关键词与状态筛选。
     *
     * @param keyword 关键词，可空
     * @param status  状态，可空
     * @param offset  偏移量
     * @param limit   条数
     * @return 用户实体列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<UserEntity> selectAdminPage(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /**
     * 管理端用户分页总数，条件与 {@link #selectAdminPage} 一致。
     *
     * @param keyword 关键词，可空
     * @param status  状态，可空
     * @return 总数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countAdminPage(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 更新用户启停状态。
     *
     * @param id     用户 ID
     * @param status 目标状态
     * @return 影响行数
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计启用状态用户数，供仪表盘展示。
     *
     * @return 启用用户数量
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    long countActive();
}
