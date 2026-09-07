package cn.miyf.kitchen.service;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.model.User;
import cn.miyf.kitchen.bean.qo.UserPageQo;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;

/**
 * 厨房用户应用服务（管理端只读）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
public class UserApplicationService extends BaseApplicationService {

    private final UserRepository userRepository;

    /**
     * 构造服务。
     *
     * @param userRepository 用户仓储
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public UserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 管理端用户分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public PageResult<UserVo> pageAdmin(UserPageQo qo) {
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        PageResult<User> result = userRepository.page(qo.getKeyword(), qo.getStatus(), page, rows);
        return PageResult.of(result.records().stream().map(this::toVo).toList(),
                result.total(), result.page(), result.pageSize());
    }

    /**
     * 管理端用户详情。
     *
     * @param id 用户 ID
     * @return VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public UserVo getAdmin(Long id) {
        return toVo(requireById(userRepository, id, "用户不存在"));
    }

    private UserVo toVo(User user) {
        return new UserVo()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setAvatarUrl(user.getAvatarUrl())
                .setPhone(user.getPhone())
                .setWechatId(user.getWechatId())
                .setOpenId(user.getOpenid())
                .setStatus(user.getStatus())
                .setCreateTime(user.getCreateTime())
                .setLastLoginTime(null);
    }
}
