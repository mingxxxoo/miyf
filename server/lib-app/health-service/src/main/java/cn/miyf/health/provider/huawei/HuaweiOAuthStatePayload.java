package cn.miyf.health.provider.huawei;

import cn.miyf.auth.security.PrincipalType;

/**
 * OAuth state 载荷：绑定健康主体、发起授权方与本次 redirect_uri，用于防 CSRF 与换票。
 * 字段由早期的 adminId 扩展为 principalId + principalType，并携带 redirectUri。
 *
 * @param subjectId     健康主体
 * @param principalId   发起授权的管理员或用户 ID
 * @param principalType 发起方类型
 * @param redirectUri   授权时使用的回调地址（换票必须一致）
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
public record HuaweiOAuthStatePayload(
        Long subjectId,
        Long principalId,
        PrincipalType principalType,
        String redirectUri
) {
}
