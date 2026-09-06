package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.provider.HuaweiHealthDataProvider;
import cn.miyf.health.service.HealthApplicationService;
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
    private final HealthApplicationService healthApplicationService;

    public HuaweiHealthAuthFacade(HuaweiHealthOAuthService oauthService,
                                  HealthApplicationService healthApplicationService) {
        this.oauthService = oauthService;
        this.healthApplicationService = healthApplicationService;
    }

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

    public Map<String, Object> completeOAuth(Long subjectId, String code, String state) {
        HuaweiHealthOAuthService.OAuthResult result = oauthService.exchangeCode(subjectId, code, state);
        return bindAndRespond(result.subjectId(), result.token());
    }

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
        map.put("expiresAt", bundle == null || bundle.getExpiresAt() == null
                ? null : bundle.getExpiresAt().toString());
        return map;
    }

    public void revoke(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        oauthService.clearToken(subjectId);
        healthApplicationService.upsertBinding(subjectId, new HealthProviderBindingSaveDto()
                .setProviderCode(HuaweiHealthDataProvider.CODE)
                .setExternalAccountId(null)
                .setCredentialRef(null)
                .setStatus("REVOKED"));
    }

    private Map<String, Object> bindAndRespond(Long sid, HuaweiTokenBundle bundle) {
        String openId = StringUtils.hasText(bundle.getOpenId())
                ? bundle.getOpenId()
                : ("huawei-" + sid);
        healthApplicationService.upsertBinding(sid, new HealthProviderBindingSaveDto()
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
        map.put("expiresAt", bundle.getExpiresAt() == null ? null : bundle.getExpiresAt().toString());
        return map;
    }

    private static String mask(String v) {
        if (!StringUtils.hasText(v) || v.length() < 6) {
            return v;
        }
        return v.substring(0, 2) + "***" + v.substring(v.length() - 2);
    }
}
