package cn.miyf.health.provider.huawei;

/**
 * OAuth state 载荷：绑定主体与发起授权的管理员，防 CSRF。
 *
 * @param subjectId 健康主体
 * @param adminId   发起授权的管理员
 * @author XieMingJie
 * @since 2026-09-09
 */
public record HuaweiOAuthStatePayload(Long subjectId, Long adminId) {
}
