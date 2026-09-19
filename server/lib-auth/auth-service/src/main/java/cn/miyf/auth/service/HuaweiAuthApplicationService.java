package cn.miyf.auth.service;

import cn.miyf.auth.bean.dto.HuaweiLoginDto;
import cn.miyf.auth.bean.model.AppUserAccount;
import cn.miyf.auth.bean.vo.LoginVo;
import cn.miyf.auth.infrastructure.huawei.HuaweiAccountClient;
import cn.miyf.auth.infrastructure.huawei.HuaweiIdentity;
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
import java.util.regex.Pattern;

/**
 * 华为账号登录编排：换票、按 unionId 查找、手机号唯一时合并、签发与微信登录相同的 JWT。
 * <p>
 * 纯华为用户的 openid 写成 {@code hw:} 前缀，满足 kitchen_user.openid 非空唯一，不占用微信 openid。
 * 多个账号共用同一手机号时拒绝自动合并，避免绑错人。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@Service
@RequiredArgsConstructor
public class HuaweiAuthApplicationService extends BaseApplicationService implements HuaweiLoginService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final int OPENID_MAX = 128;

    private final AppUserAccountStore appUserAccountStore;
    private final AppUserAuthAuthorityLoader appUserAuthAuthorityLoader;
    private final JwtService jwtService;
    private final HuaweiAccountClient huaweiAccountClient;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    @Override
    @Transactional
    public LoginVo huaweiLogin(HuaweiLoginDto dto) {
        String principalKey = null;
        try {
            HuaweiIdentity identity = huaweiAccountClient.exchange(dto.getAuthorizationCode());
            principalKey = "hw:" + identity.unionId();
            redisRateLimiter.assertLoginAllowed(principalKey);
            AppUserAccount user = resolveAccount(identity);
            if (!"ENABLED".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            redisRateLimiter.clearLoginFailures(principalKey);
            return issue(user);
        } catch (BusinessException ex) {
            if (principalKey != null && (ex.getCode() == ErrorCode.HUAWEI_AUTH_FAILED.getCode()
                    || ex.getCode() == ErrorCode.USER_DISABLED.getCode()
                    || ex.getCode() == ErrorCode.CONFLICT.getCode())) {
                redisRateLimiter.recordLoginFailure(principalKey);
            }
            throw ex;
        }
    }

    /**
     * 先按华为 unionId，再按唯一手机号；都没有则注册。
     */
    private AppUserAccount resolveAccount(HuaweiIdentity identity) {
        return appUserAccountStore.findByHuaweiUnionId(identity.unionId())
                .orElseGet(() -> mergeOrRegister(identity));
    }

    private AppUserAccount mergeOrRegister(HuaweiIdentity identity) {
        String phone = normalizePhone(identity.phone());
        if (phone != null) {
            List<AppUserAccount> matched = appUserAccountStore.findByPhone(phone);
            if (matched.size() > 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "该手机号对应多个账号，无法自动合并");
            }
            if (matched.size() == 1) {
                return bindExisting(matched.get(0), identity, phone);
            }
        }
        return register(identity, phone);
    }

    /**
     * 合并时保留原微信 openid，只补华为标识和空着的昵称、手机号。
     */
    private AppUserAccount bindExisting(AppUserAccount user, HuaweiIdentity identity, String phone) {
        if (StringUtils.hasText(user.getHuaweiUnionId())
                && !user.getHuaweiUnionId().equals(identity.unionId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已绑定其他华为账号");
        }
        user.setHuaweiOpenId(identity.openId());
        user.setHuaweiUnionId(identity.unionId());
        if (!StringUtils.hasText(user.getPhone())) {
            user.setPhone(phone);
        }
        if (!StringUtils.hasText(user.getNickname()) && StringUtils.hasText(identity.displayName())) {
            user.setNickname(identity.displayName().trim());
        }
        return appUserAccountStore.save(user);
    }

    private AppUserAccount register(HuaweiIdentity identity, String phone) {
        String name = StringUtils.hasText(identity.displayName())
                ? identity.displayName().trim()
                : defaultName(identity.openId());
        return appUserAccountStore.save(new AppUserAccount()
                .setOpenid(syntheticOpenid(identity.openId()))
                .setHuaweiOpenId(identity.openId())
                .setHuaweiUnionId(identity.unionId())
                .setUsername(name)
                .setNickname(name)
                .setPhone(phone)
                .setStatus("ENABLED"));
    }

    private LoginVo issue(AppUserAccount user) {
        String displayName = StringUtils.hasText(user.getNickname())
                ? user.getNickname()
                : (StringUtils.hasText(user.getUsername()) ? user.getUsername() : "华为用户");
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
    }

    /**
     * openid 列最长 128。超长时保留末尾，仍以 hw: 区分微信 openid。
     */
    static String syntheticOpenid(String openId) {
        String raw = "hw:" + openId;
        if (raw.length() <= OPENID_MAX) {
            return raw;
        }
        return "hw:" + openId.substring(openId.length() - (OPENID_MAX - 3));
    }

    private static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String trimmed = phone.trim();
        return PHONE.matcher(trimmed).matches() ? trimmed : null;
    }

    private static String defaultName(String openId) {
        String suffix = openId.length() <= 4 ? openId : openId.substring(openId.length() - 4);
        return "华为用户" + suffix;
    }
}
