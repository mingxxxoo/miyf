package cn.miyf.auth.service;

import cn.miyf.auth.bean.dto.WxLoginDto;
import cn.miyf.auth.bean.model.AppUserAccount;
import cn.miyf.auth.bean.vo.LoginVo;
import cn.miyf.auth.infrastructure.wx.WxAuthClient;
import cn.miyf.auth.infrastructure.wx.WxSession;
import cn.miyf.auth.security.AppUserAuthAuthorityLoader;
import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.auth.security.JwtService;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.spi.AppUserAccountStore;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.infrastructure.redis.RedisRateLimiter;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 用户端微信登录编排：换票、账号 upsert、限流、签发 JWT。
 * <p>
 * 登录成功后挂载各产品域默认角色（如 kitchen/health 的 {@code default_person}）及其「个人」API 权限码。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Service
@RequiredArgsConstructor
public class WxAuthApplicationService extends BaseApplicationService implements WxLoginService {

    private final AppUserAccountStore appUserAccountStore;
    private final AppUserAuthAuthorityLoader appUserAuthAuthorityLoader;
    private final JwtService jwtService;
    private final WxAuthClient wxAuthClient;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * 微信小程序登录：账号不存在则自动注册。
     * <p>
     * 仅需 wx.login 的 code；用户名 / 手机号 / 微信号可选。失败计数按 openid 维度限流。
     * 签发 JWT 时写入默认角色对应的个人端权限码（与管理员 createUser 绑定 default_person 对齐）。
     *
     * @param dto 登录请求
     * @return 登录结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    @Transactional
    public LoginVo wxLogin(WxLoginDto dto) {
        String principalKey = null;
        try {
            WxSession session = wxAuthClient.code2Session(dto.getCode());
            principalKey = "wx:" + session.openid();
            redisRateLimiter.assertLoginAllowed(principalKey);
            AppUserAccount user = appUserAccountStore.findByOpenid(session.openid())
                    .orElseGet(() -> registerNewUser(session, dto));
            if (!"ENABLED".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            user = applyOptionalProfile(user, dto);
            String displayName = StringUtils.hasText(user.getNickname())
                    ? user.getNickname()
                    : (StringUtils.hasText(user.getUsername()) ? user.getUsername() : user.getOpenid());
            // kitchen_user 不落 sys_user_role；每次登录按 is_default 角色解析个人权限写入 JWT
            List<String> roleCodes = appUserAuthAuthorityLoader.loadDefaultRoleCodes();
            List<String> permissionCodes = appUserAuthAuthorityLoader.loadDefaultPermissionCodes();
            DataScope dataScope = DataScope.parse(appUserAuthAuthorityLoader.loadDefaultDataScope());
            AuthPrincipal principal = new AuthPrincipal(
                    user.getId(),
                    displayName,
                    PrincipalType.USER,
                    permissionCodes,
                    true,
                    null,
                    dataScope
            );
            redisRateLimiter.clearLoginFailures(principalKey);
            return new LoginVo()
                    .setToken(jwtService.createToken(principal))
                    .setExpireSeconds(jwtService.getExpireSeconds())
                    .setUserId(principal.getId())
                    .setDisplayName(displayName)
                    .setUsername(user.getUsername())
                    .setAvatarUrl(user.getAvatarUrl())
                    .setPrincipalType(principal.getType().name())
                    .setPermissions(principal.getPermissions())
                    .setRoles(roleCodes);
        } catch (BusinessException ex) {
            if (principalKey != null && (ex.getCode() == ErrorCode.WX_AUTH_FAILED.getCode()
                    || ex.getCode() == ErrorCode.LOGIN_FAILED.getCode()
                    || ex.getCode() == ErrorCode.USER_DISABLED.getCode())) {
                redisRateLimiter.recordLoginFailure(principalKey);
            }
            throw ex;
        }
    }

    private AppUserAccount registerNewUser(WxSession session, WxLoginDto dto) {
        String defaultName = defaultDisplayName(session.openid());
        String username = StringUtils.hasText(dto.getUsername()) ? dto.getUsername().trim() : defaultName;
        String nickname = StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : username;
        return appUserAccountStore.save(new AppUserAccount()
                .setOpenid(session.openid())
                .setUnionid(session.unionid())
                .setUsername(username)
                .setNickname(nickname)
                .setPhone(trimToNull(dto.getPhone()))
                .setWechatId(trimToNull(dto.getWechatId()))
                .setAvatarUrl(StringUtils.hasText(dto.getAvatarUrl())
                        ? normalizeLoginAvatarUrl(dto.getAvatarUrl().trim()) : null)
                .setStatus("ENABLED"));
    }

    private AppUserAccount applyOptionalProfile(AppUserAccount user, WxLoginDto dto) {
        boolean profileChanged = false;
        if (StringUtils.hasText(dto.getUsername()) && !Objects.equals(dto.getUsername().trim(), user.getUsername())) {
            user.setUsername(dto.getUsername().trim());
            profileChanged = true;
        }
        if (StringUtils.hasText(dto.getNickname()) && !Objects.equals(dto.getNickname().trim(), user.getNickname())) {
            user.setNickname(dto.getNickname().trim());
            profileChanged = true;
        }
        if (StringUtils.hasText(dto.getPhone()) && !Objects.equals(dto.getPhone().trim(), user.getPhone())) {
            user.setPhone(dto.getPhone().trim());
            profileChanged = true;
        }
        if (StringUtils.hasText(dto.getWechatId()) && !Objects.equals(dto.getWechatId().trim(), user.getWechatId())) {
            user.setWechatId(dto.getWechatId().trim());
            profileChanged = true;
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            String normalized = normalizeLoginAvatarUrl(dto.getAvatarUrl().trim());
            if (normalized != null && !Objects.equals(normalized, user.getAvatarUrl())) {
                user.setAvatarUrl(normalized);
                profileChanged = true;
            }
        }
        return profileChanged ? appUserAccountStore.save(user) : user;
    }

    /**
     * 登录资料头像：仅允许相对本站 /r/、/api/r/；绝对 URL 与危险协议一律忽略或拒绝。
     * 微信临时路径 / 外站 CDN 不落库，由客户端登录后走上传接口。
     */
    private static String normalizeLoginAvatarUrl(String raw) {
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的头像地址协议");
        }
        // 不接受任意 host 的绝对 URL（避免 evil.com/r/xx 被抽 path 入库）
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return null;
        }
        String path = raw.trim();
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        if (path.startsWith("/r/") || path.startsWith("/api/r/")) {
            return path;
        }
        // 微信 chooseAvatar 本地临时路径：忽略
        return null;
    }

    private static String defaultDisplayName(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "微信用户";
        }
        String suffix = openid.length() <= 4 ? openid : openid.substring(openid.length() - 4);
        return "微信用户" + suffix;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
