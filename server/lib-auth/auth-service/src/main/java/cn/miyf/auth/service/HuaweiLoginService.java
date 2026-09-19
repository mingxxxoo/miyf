package cn.miyf.auth.service;

import cn.miyf.auth.bean.dto.HuaweiLoginDto;
import cn.miyf.auth.bean.vo.LoginVo;

/**
 * 鸿蒙华为账号登录门面。签发的 JWT 与微信登录相同，业务接口不区分登录方式。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
public interface HuaweiLoginService {

    /**
     * 用授权码登录或注册，并在手机号唯一时合并到已有用户。
     *
     * @param dto 授权码
     * @return 与微信登录相同结构的登录结果
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    LoginVo huaweiLogin(HuaweiLoginDto dto);
}
