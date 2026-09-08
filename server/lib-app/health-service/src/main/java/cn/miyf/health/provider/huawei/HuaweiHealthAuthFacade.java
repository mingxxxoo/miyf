package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.provider.HuaweiHealthDataProvider;
import cn.miyf.health.service.HealthCrudApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 华为授权编排：仅 OAuth，按健康主体独立绑定，互不共用。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class HuaweiHealthAuthFacade {

    private final HuaweiHealthOAuthService oauthService;
    private final HealthCrudApplicationService healthCrudApplicationService;

    /**
     * 构造门面。
     *
     * @param oauthService                 OAuth
     * @param healthCrudApplicationService CRUD（绑定 upsert）
     * @history 1.00 2026-09-08 XieMingJie Use HealthCrudApplicationService.
     */
    public HuaweiHealthAuthFacade(HuaweiHealthOAuthService oauthService,
                                  HealthCrudApplicationService healthCrudApplicationService) {
        this.oauthService = oauthService;
        this.healthCrudApplicationService = healthCrudApplicationService;
    }

    /**
     * 生成华为授权 URL。
     *
     * @param subjectId 健康主体
     * @return 含 authorizeUrl / subjectId / providerCode
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> authorizeUrl(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        String url = oauthService.buildAuthorizeUrl(subjectId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authorizeUrl", url);
        map.put("subjectId", String.valueOf(subjectId));
        map.put("providerCode", HuaweiHealthDataProvider.CODE);
        return map;
    }

    /**
     * 完成 OAuth 回调：换票并 upsert ACTIVE 绑定。
     *
     * @param subjectId 主体（可空，由 state 解析）
     * @param code      授权码
     * @param state     OAuth state
     * @return 授权结果摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> completeOAuth(Long subjectId, String code, String state) {
        HuaweiHealthOAuthService.OAuthResult result = oauthService.exchangeCode(subjectId, code, state);
        return bindAndRespond(result.subjectId(), result.token());
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
        oauthService.clearToken(subjectId);
        healthCrudApplicationService.upsertBinding(subjectId, new HealthProviderBindingSaveDto()
                .setProviderCode(HuaweiHealthDataProvider.CODE)
                .setExternalAccountId(null)
                .setCredentialRef(null)
                .setStatus("REVOKED"));
    }

    private Map<String, Object> bindAndRespond(Long sid, HuaweiTokenBundle bundle) {
        String openId = StringUtils.hasText(bundle.getOpenId())
                ? bundle.getOpenId()
                : ("huawei-" + sid);
        healthCrudApplicationService.upsertBinding(sid, new HealthProviderBindingSaveDto()
                .setProviderCode(HuaweiHealthDataProvider.CODE)
                .setExternalAccountId(openId)
                .setCredentialRef(oauthService.credentialRef(sid))
                .setStatus("ACTIVE"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("subjectId", String.valueOf(sid));
        map.put("providerCode", HuaweiHealthDataProvider.CODE);
        map.put("authorized", true);
        map.put("source", bundle.getSource());
        map.put("openId", openId);
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
