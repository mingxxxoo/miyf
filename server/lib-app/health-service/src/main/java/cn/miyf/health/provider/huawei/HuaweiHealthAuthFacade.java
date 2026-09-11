package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.provider.HuaweiHealthDataProvider;
import cn.miyf.health.service.HealthCrudApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 华为授权编排：仅 OAuth，按健康主体独立绑定，互不共用。
 * 支持管理端 SPA 回调与个人端浏览器无会话回跳两种完成方式。
 *
 * @author XieMingJie
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class HuaweiHealthAuthFacade {

    private final HuaweiHealthOAuthService oauthService;
    private final HealthCrudApplicationService healthCrudApplicationService;

    /**
     * 生成管理端华为授权 URL。
     * 个人端请使用 {@link #authorizeUrlForUser(Long)}。
     *
     * @param subjectId 健康主体
     * @return 含 authorizeUrl / subjectId / providerCode
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> authorizeUrl(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        healthCrudApplicationService.requireAccessibleSubject(subjectId);
        return wrapAuthorizeUrl(subjectId, oauthService.buildAuthorizeUrl(subjectId));
    }

    /**
     * 生成个人端华为授权 URL（绑定「我的」主体）。
     *
     * @param subjectId 健康主体
     * @return 含 authorizeUrl / subjectId / providerCode
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public Map<String, Object> authorizeUrlForUser(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        healthCrudApplicationService.requireAccessibleSubject(subjectId);
        return wrapAuthorizeUrl(subjectId, oauthService.buildUserAuthorizeUrl(subjectId));
    }

    /**
     * 完成 OAuth 回调：换票并 upsert ACTIVE 绑定（须当前登录与 state 发起方一致）。
     * 浏览器无会话回跳请使用 {@link #completeOAuthFromRedirect(String, String)}。
     *
     * @param subjectId 主体（可空，由 state 解析）
     * @param code      授权码
     * @param state     OAuth state
     * @return 授权结果摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> completeOAuth(Long subjectId, String code, String state) {
        HuaweiHealthOAuthService.OAuthResult result = oauthService.exchangeCode(subjectId, code, state);
        healthCrudApplicationService.requireAccessibleSubject(result.subjectId());
        return bindAndRespond(result.subjectId(), result.token());
    }

    /**
     * 浏览器回跳换票：消费 state、落库绑定（无需登录会话）。
     * 绑定走 {@link HealthCrudApplicationService#upsertBindingAfterOAuthRedirect}，避免依赖登录上下文。
     *
     * @param code  授权码
     * @param state OAuth state
     * @return 授权结果摘要
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public Map<String, Object> completeOAuthFromRedirect(String code, String state) {
        HuaweiHealthOAuthService.OAuthResult result = oauthService.exchangeCodeFromRedirect(code, state);
        return bindAndRespond(result.subjectId(), result.token(), true);
    }

    private Map<String, Object> wrapAuthorizeUrl(Long subjectId, String url) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authorizeUrl", url);
        map.put("subjectId", String.valueOf(subjectId));
        map.put("providerCode", HuaweiHealthDataProvider.CODE);
        return map;
    }

    /**
     * 查询主体华为授权状态（openId 脱敏）。
     *
     * @param subjectId 主体
     * @return 状态摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> status(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        healthCrudApplicationService.requireAccessibleSubject(subjectId);
        HuaweiTokenBundle bundle = oauthService.peekToken(subjectId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("subjectId", String.valueOf(subjectId));
        map.put("providerCode", HuaweiHealthDataProvider.CODE);
        map.put("authorized", bundle != null && StringUtils.hasText(bundle.getAccessToken()));
        map.put("source", bundle == null ? null : bundle.getSource());
        map.put("hasRefreshToken", bundle != null && StringUtils.hasText(bundle.getRefreshToken()));
        map.put("openId", bundle == null ? null : mask(bundle.getOpenId()));
        map.put("expiresTime", bundle == null || bundle.getExpiresTime() == null
                ? null : bundle.getExpiresTime().toString());
        return map;
    }

    /**
     * 撤销授权：清 token，并将绑定置为 REVOKED。
     *
     * @param subjectId 主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void revoke(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        healthCrudApplicationService.requireAccessibleSubject(subjectId);
        oauthService.clearToken(subjectId);
        healthCrudApplicationService.upsertBinding(subjectId, new HealthProviderBindingSaveDto()
                .setProviderCode(HuaweiHealthDataProvider.CODE)
                .setExternalAccountId(null)
                .setCredentialRef(null)
                .setStatus("REVOKED"));
    }

    private Map<String, Object> bindAndRespond(Long sid, HuaweiTokenBundle bundle) {
        return bindAndRespond(sid, bundle, false);
    }

    /**
     * 换票后 upsert ACTIVE 绑定并返回脱敏摘要。
     *
     * @param sid           主体
     * @param bundle        Token
     * @param oauthRedirect true 表示浏览器无会话回跳，跳过登录态访问校验
     * @return 授权结果摘要
     */
    private Map<String, Object> bindAndRespond(Long sid, HuaweiTokenBundle bundle, boolean oauthRedirect) {
        String openId = StringUtils.hasText(bundle.getOpenId())
                ? bundle.getOpenId()
                : ("huawei-" + sid);
        HealthProviderBindingSaveDto dto = new HealthProviderBindingSaveDto()
                .setProviderCode(HuaweiHealthDataProvider.CODE)
                .setExternalAccountId(openId)
                .setCredentialRef(oauthService.credentialRef(sid))
                .setStatus("ACTIVE");
        if (oauthRedirect) {
            healthCrudApplicationService.upsertBindingAfterOAuthRedirect(sid, dto);
        } else {
            healthCrudApplicationService.upsertBinding(sid, dto);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("subjectId", String.valueOf(sid));
        map.put("providerCode", HuaweiHealthDataProvider.CODE);
        map.put("authorized", true);
        map.put("source", bundle.getSource());
        map.put("openId", mask(openId));
        map.put("expiresTime", bundle.getExpiresTime() == null ? null : bundle.getExpiresTime().toString());
        return map;
    }

    private static String mask(String v) {
        if (!StringUtils.hasText(v) || v.length() < 6) {
            return v;
        }
        return v.substring(0, 2) + "***" + v.substring(v.length() - 2);
    }
}
