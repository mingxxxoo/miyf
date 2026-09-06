package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.User;
import cn.miyf.repository.BaseRepository;

import java.util.Optional;

/**
 * 微信用户仓储。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public interface UserRepository extends BaseRepository<User, Long> {

    /**
     * 按 openid 查询用户。
     *
     * @param openid 微信 openid
     * @return 用户
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    Optional<User> findByOpenid(String openid);

    /**
     * 管理端分页查询用户。
     *
     * @param keyword  昵称关键词
     * @param status   状态
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    PageResult<User> page(String keyword, String status, long page, long pageSize);

    /**
     * 更新用户启停状态。
     *
     * @param id     用户 ID
     * @param status 目标状态
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    void updateStatus(Long id, String status);

    /**
     * 统计启用用户数。
     *
     * @return 数量
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    long countActive();
}
