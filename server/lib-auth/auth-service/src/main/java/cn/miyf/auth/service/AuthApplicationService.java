package cn.miyf.auth.service;

import cn.miyf.auth.bean.dto.AdminLoginDto;
import cn.miyf.auth.bean.entity.SysUserEntity;
import cn.miyf.auth.bean.vo.CaptchaVo;
import cn.miyf.auth.bean.vo.LoginRiskVo;
import cn.miyf.auth.bean.vo.LoginVo;
import cn.miyf.auth.repository.mapper.SysUserMapper;
import cn.miyf.auth.security.AdminAuthAuthorityLoader;
import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.auth.security.JwtService;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.service.BaseApplicationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台认证应用服务：管理员（sys_user）登录。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Service
public class AuthApplicationService extends BaseApplicationService {

    private final SysUserMapper sysUserMapper;
    private final AdminAuthAuthorityLoader authorityLoader;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginProtectService loginProtectService;

    /**
     * 构造认证服务：接入验证码与阶梯封禁，权限加载委托 {@link AdminAuthAuthorityLoader}。
     *
     * @param sysUserMapper       系统用户 Mapper
     * @param authorityLoader     角色/权限加载
     * @param passwordEncoder     密码编码器
     * @param jwtService          JWT 服务
     * @param loginProtectService 登录风控
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthApplicationService(SysUserMapper sysUserMapper,
                                  AdminAuthAuthorityLoader authorityLoader,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  LoginProtectService loginProtectService) {
        this.sysUserMapper = sysUserMapper;
        this.authorityLoader = authorityLoader;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginProtectService = loginProtectService;
    }

    /**
     * 查询登录风控状态。
     *
     * @param username 用户名
     * @return 状态
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public LoginRiskVo loginStatus(String username) {
        return loginProtectService.status(principalKey(username));
    }

    /**
     * 下发图形验证码。
     *
     * @return 验证码
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public CaptchaVo createCaptcha() {
        return loginProtectService.createCaptcha();
    }

    /**
     * 管理员登录（sys_user）：校验验证码与阶梯封禁后再鉴权发令牌。
     *
     * @param dto 登录请求
     * @return 登录结果
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public LoginVo adminLogin(AdminLoginDto dto) {
        String principalKey = principalKey(dto.getUsername());
        loginProtectService.assertCanAttempt(principalKey, dto.getCaptchaId(), dto.getCaptchaCode());
        try {
            SysUserEntity user = sysUserMapper.selectByUsername(dto.getUsername());
            if (user == null) {
                throw loginFailed(principalKey);
            }
            if (!"ENABLED".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
                throw loginFailed(principalKey);
            }
            List<String> roleCodes = authorityLoader.loadRoleCodes(user.getId());
            List<String> permissionCodes = authorityLoader.loadPermissionCodes(user.getId());
            DataScope dataScope = DataScope.parse(authorityLoader.loadEffectiveDataScope(user.getId()));
            AuthPrincipal principal = new AuthPrincipal(
                    user.getId(),
                    user.getUsername(),
                    PrincipalType.ADMIN,
                    permissionCodes,
                    true,
                    user.getOrgUnitId(),
                    dataScope
            );
            loginProtectService.clear(principalKey);
            return toLoginVo(principal, user.getNickname(), user.getUsername(), roleCodes);
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.LOGIN_FAILED.getCode() && ex.getData() == null) {
                throw loginFailed(principalKey);
            }
            throw ex;
        }
    }

    private BusinessException loginFailed(String principalKey) {
        LoginRiskVo risk = loginProtectService.recordFailure(principalKey);
        String message = ErrorCode.LOGIN_FAILED.getMessage();
        if (risk.isLocked()) {
            return new BusinessException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "登录失败次数过多，账号已临时锁定",
                    risk);
        }
        if (risk.isCaptchaRequired()) {
            message = "用户名或密码错误，请输入验证码后重试";
        }
        return new BusinessException(ErrorCode.LOGIN_FAILED, message, risk);
    }

    private static String principalKey(String username) {
        return "admin:" + (username == null ? "" : username.trim().toLowerCase());
    }

    /**
     * 组装登录响应。
     *
     * @param principal   主体
     * @param displayName 展示名
     * @param roles       角色列表
     * @return LoginVo
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    private LoginVo toLoginVo(AuthPrincipal principal, String displayName, String username, List<String> roles) {
        return new LoginVo()
                .setToken(jwtService.createToken(principal))
                .setExpireSeconds(jwtService.getExpireSeconds())
                .setUserId(principal.getId())
                .setDisplayName(displayName)
                .setUsername(username)
                .setPrincipalType(principal.getType().name())
                .setPermissions(principal.getPermissions())
                .setRoles(roles);
    }
}
