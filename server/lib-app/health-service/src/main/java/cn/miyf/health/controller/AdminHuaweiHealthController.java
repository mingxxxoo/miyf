package cn.miyf.health.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.bean.dto.HuaweiOAuthCallbackDto;
import cn.miyf.health.provider.huawei.HuaweiHealthAuthFacade;
import cn.miyf.health.security.HealthPersonalPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 华为 Health Kit OAuth 授权接口。
 * 按健康主体独立绑定，主体之间不共用 Token。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "健康-华为授权")
@HealthPersonalPopedom
@RestController
@RequestMapping("/admin/health/providers/huawei")
@RequiredArgsConstructor
public class AdminHuaweiHealthController {

    private final HuaweiHealthAuthFacade huaweiHealthAuthFacade;

    /**
     * 生成绑定到指定主体的华为 OAuth 授权 URL。
     *
     * @param subjectId 健康主体 ID
     * @return 含 authorizeUrl 等字段的结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "获取华为 OAuth 授权 URL（绑定到指定主体）")
    @MiyfPermission(code = "health:huawei:oauth")
    @GetMapping("/authorize-url")
    public ApiResult<Map<String, Object>> authorizeUrl(@RequestParam Long subjectId) {
        return ApiResult.ok(huaweiHealthAuthFacade.authorizeUrl(subjectId));
    }

    /**
     * 使用授权码换取 Token，并写入该主体的绑定关系。
     *
     * @param dto 回调参数（code 必填；subjectId / state 可选）
     * @return 授权结果摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "用授权码换取 Token 并绑定主体")
    @MiyfPermission(code = "health:huawei:oauth")
    @PostMapping("/oauth/callback")
    public ApiResult<Map<String, Object>> oauthCallback(@Valid @RequestBody HuaweiOAuthCallbackDto dto) {
        return ApiResult.ok(huaweiHealthAuthFacade.completeOAuth(
                parseOptionalSubjectId(dto.getSubjectId()),
                dto.getCode(),
                dto.getState()));
    }

    /**
     * 查询指定主体的华为授权状态（是否已授权、过期时间等）。
     *
     * @param subjectId 健康主体 ID
     * @return 状态摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "查询主体华为授权状态")
    @MiyfPermission(code = "health:huawei:oauth")
    @GetMapping("/oauth/status")
    public ApiResult<Map<String, Object>> oauthStatus(@RequestParam Long subjectId) {
        return ApiResult.ok(huaweiHealthAuthFacade.status(subjectId));
    }

    /**
     * 撤销指定主体的华为授权并清理本地绑定凭证。
     *
     * @param subjectId 健康主体 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "撤销指定主体的华为授权")
    @MiyfPermission(code = "health:huawei:oauth")
    @DeleteMapping("/oauth")
    public ApiResult<Void> revoke(@RequestParam Long subjectId) {
        huaweiHealthAuthFacade.revoke(subjectId);
        return ApiResult.ok();
    }

    private static Long parseOptionalSubjectId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 非法");
        }
    }
}
