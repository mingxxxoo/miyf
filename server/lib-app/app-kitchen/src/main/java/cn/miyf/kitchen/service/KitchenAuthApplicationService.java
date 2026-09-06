package cn.miyf.kitchen.service;

import cn.miyf.bean.dto.WxLoginDto;
import cn.miyf.bean.vo.LoginVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.infrastructure.redis.RedisRateLimiter;
import cn.miyf.infrastructure.wx.WxAuthClient;
import cn.miyf.infrastructure.wx.WxSession;
import cn.miyf.kitchen.bean.model.User;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.JwtService;
import cn.miyf.security.PrincipalType;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 厨房用户端认证：微信登录（kitchen_user）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Service
public class KitchenAuthApplicationService extends BaseApplicationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final WxAuthClient wxAuthClient;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * 构造厨房认证服务。
     *
     * @param userRepository   厨房用户仓储
     * @param jwtService       JWT
     * @param wxAuthClient     微信客户端
     * @param redisRateLimiter 登录限流
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public KitchenAuthApplicationService(UserRepository userRepository,
                                         JwtService jwtService,
                                         WxAuthClient wxAuthClient,
                                         RedisRateLimiter redisRateLimiter) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.wxAuthClient = wxAuthClient;
        this.redisRateLimiter = redisRateLimiter;
    }

    /**
     * 微信小程序登录：不存在则自动注册。
     *
     * @param dto 登录请求
     * @return 登录结果
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public LoginVo wxLogin(WxLoginDto dto) {
        String principalKey = "wx:" + dto.getCode();
        redisRateLimiter.assertLoginAllowed(principalKey);
        try {
            WxSession session = wxAuthClient.code2Session(dto.getCode());
            User user = userRepository.findByOpenid(session.openid()).orElseGet(() -> {
                User created = new User();
                created.setOpenid(session.openid());
                created.setUnionid(session.unionid());
                created.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "厨房新朋友");
                created.setAvatarUrl(dto.getAvatarUrl());
                created.setStatus("ENABLED");
                return userRepository.save(created);
            });
            if (!"ENABLED".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            boolean profileChanged = false;
            if (StringUtils.hasText(dto.getNickname()) && !dto.getNickname().equals(user.getNickname())) {
                user.setNickname(dto.getNickname());
                profileChanged = true;
            }
            if (StringUtils.hasText(dto.getAvatarUrl()) && !dto.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(dto.getAvatarUrl());
                profileChanged = true;
            }
            if (profileChanged) {
                userRepository.save(user);
            }
            AuthPrincipal principal = new AuthPrincipal(
                    user.getId(),
                    user.getNickname() == null ? user.getOpenid() : user.getNickname(),
                    PrincipalType.USER,
                    List.of(),
                    true
            );
            redisRateLimiter.clearLoginFailures(principalKey);
            return new LoginVo()
                    .setToken(jwtService.createToken(principal))
                    .setExpireSeconds(jwtService.getExpireSeconds())
                    .setUserId(principal.getId())
                    .setDisplayName(user.getNickname())
                    .setPrincipalType(principal.getType().name())
                    .setPermissions(principal.getPermissions())
                    .setRoles(List.of());
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.WX_AUTH_FAILED.getCode()
                    || ex.getCode() == ErrorCode.LOGIN_FAILED.getCode()) {
                redisRateLimiter.recordLoginFailure(principalKey);
            }
            throw ex;
        }
    }
}
