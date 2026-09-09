package cn.miyf.auth.service;

import cn.miyf.auth.bean.dto.WxLoginDto;
import cn.miyf.auth.bean.vo.LoginVo;

/**
 * 用户端微信登录门面。
 * <p>
 * 由业务模块（如 kitchen）提供实现；auth-service 只暴露 HTTP 入口。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public interface WxLoginService {

    /**
     * 微信小程序登录（仅需 wx.login code；资料字段可选）。
     *
     * @param dto 登录请求
     * @return 登录结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    LoginVo wxLogin(WxLoginDto dto);
}
