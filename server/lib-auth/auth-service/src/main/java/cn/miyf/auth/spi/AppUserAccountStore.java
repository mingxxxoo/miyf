package cn.miyf.auth.spi;

import cn.miyf.auth.bean.model.AppUserAccount;

import java.util.Optional;

/**
 * 用户端账号存储 SPI：由业务模块（如 kitchen）提供实现。
 * <p>
 * auth-service 负责登录编排（微信换票、限流、发 JWT），不感知具体业务表。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public interface AppUserAccountStore {

    /**
     * 按微信 openid 查找账号。
     *
     * @param openid 微信 openid
     * @return 账号
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    Optional<AppUserAccount> findByOpenid(String openid);

    /**
     * 新建或更新账号。
     *
     * @param account 账号
     * @return 持久化后的账号（含 ID）
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    AppUserAccount save(AppUserAccount account);
}
