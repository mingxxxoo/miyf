package cn.miyf.kitchen.spi;

import cn.miyf.auth.bean.model.AppUserAccount;
import cn.miyf.auth.spi.AppUserAccountStore;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.User;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 厨房侧用户账号存储：将 {@code kitchen_user} 适配为 auth 的 {@link AppUserAccountStore}。
 * 登录编排在 auth 模块，厨房仅提供账号读写 SPI。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Component
@RequiredArgsConstructor
public class KitchenAppUserAccountStore implements AppUserAccountStore {

    private final UserRepository userRepository;

    /**
     * 按 openid 查找厨房用户并转为 auth 账号模型。
     *
     * @param openid 微信 openid
     * @return 账号，不存在则为 empty
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public Optional<AppUserAccount> findByOpenid(String openid) {
        return Optional.ofNullable(EntityConverters.toUser(userRepository.selectByOpenid(openid)))
                .map(this::toAccount);
    }

    /**
     * 保存厨房用户；无主键插入，有主键更新。
     * 新用户默认 ENABLED，与厨房用户启停语义一致。
     *
     * @param account auth 账号
     * @return 保存后的账号
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public AppUserAccount save(AppUserAccount account) {
        User user = toUser(account);
        UserEntity entity = EntityConverters.toUserEntity(user);
        // 新用户默认启用，避免登录后无法下单
        if (entity.getStatus() == null) {
            entity.setStatus("ENABLED");
        }
        if (entity.getId() == null) {
            userRepository.insert(entity);
        } else {
            userRepository.updateById(entity);
        }
        return toAccount(EntityConverters.toUser(entity));
    }

    /**
     * 领域用户转为 auth 账号。
     *
     * @param user 厨房用户
     * @return auth 账号
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private AppUserAccount toAccount(User user) {
        return new AppUserAccount()
                .setId(user.getId())
                .setOpenid(user.getOpenid())
                .setUnionid(user.getUnionid())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setPhone(user.getPhone())
                .setWechatId(user.getWechatId())
                .setAvatarUrl(user.getAvatarUrl())
                .setStatus(user.getStatus());
    }

    /**
     * auth 账号转为厨房领域用户。
     *
     * @param account auth 账号
     * @return 厨房用户
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private User toUser(AppUserAccount account) {
        User user = new User();
        user.setId(account.getId());
        user.setOpenid(account.getOpenid());
        user.setUnionid(account.getUnionid());
        user.setUsername(account.getUsername());
        user.setNickname(account.getNickname());
        user.setPhone(account.getPhone());
        user.setWechatId(account.getWechatId());
        user.setAvatarUrl(account.getAvatarUrl());
        user.setStatus(account.getStatus());
        return user;
    }
}
