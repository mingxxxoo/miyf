package cn.miyf.auth.spi;

import cn.miyf.auth.bean.model.AppUserAccount;

import java.util.List;
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
     * 按华为 unionId 查找。微信登录不走此方法。
     *
     * @param huaweiUnionId 华为 unionId
     * @return 账号
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    Optional<AppUserAccount> findByHuaweiUnionId(String huaweiUnionId);

    /**
     * 按手机号查找，供华为登录合并。返回全部命中，由调用方判断是否唯一。
     *
     * @param phone 11 位手机号
     * @return 命中账号，无则空列表
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    List<AppUserAccount> findByPhone(String phone);

    /**
     * 新建或更新账号。
     *
     * @param account 账号
     * @return 持久化后的账号（含 ID）
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    AppUserAccount save(AppUserAccount account);
}
