package cn.miyf.kitchen.service;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.UserPageQo;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 厨房用户应用服务（管理端只读）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService extends BaseApplicationService {

    private final UserRepository userRepository;

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
        long off = offset(page, rows);
        var records = userRepository.selectAdminPage(qo.getKeyword(), qo.getStatus(), off, rows).stream()
                .map(EntityConverters::toUser)
                .map(this::toVo)
                .toList();
        long total = userRepository.countAdminPage(qo.getKeyword(), qo.getStatus());
        return PageResult.of(records, total, page, rows);
    }

    /**
     * 管理端用户详情。
     *
     * @param id 用户 ID
     * @return VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public UserVo getAdmin(Long id) {
        return toVo(EntityConverters.toUser(requireById(userRepository, id, "用户不存在")));
    }

    private UserVo toVo(cn.miyf.kitchen.bean.model.User user) {
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
