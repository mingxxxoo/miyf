package cn.miyf.health.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.bean.dto.HuaweiOAuthCallbackDto;
import cn.miyf.health.provider.huawei.HuaweiHealthAuthFacade;
import cn.miyf.health.security.HealthPersonalPopedom;
import cn.miyf.health.service.HealthCrudApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 个人端华为 Health Kit OAuth：仅绑定当前用户自己的健康主体。
 *
 * @author XieMingJie
 * @since 2026-09-10
 */
@Tag(name = "用户端-健康-华为授权")
@HealthPersonalPopedom
@RestController
@RequestMapping("/health/providers/huawei")
@RequiredArgsConstructor
public class HuaweiHealthController {

    private final HuaweiHealthAuthFacade huaweiHealthAuthFacade;
    private final HealthCrudApplicationService healthCrudApplicationService;

    /**
     * 生成绑定到「我的」主体的华为 OAuth 授权 URL。
     *
     * @return 含 authorizeUrl 等字段
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "获取我的华为 OAuth 授权 URL")
    @MiyfPermission(code = "health:user:huawei:oauth")
    @GetMapping("/authorize-url")
    public ApiResult<Map<String, Object>> authorizeUrl() {
        Long subjectId = healthCrudApplicationService.getOrCreateMySubjectEntity().getId();
        return ApiResult.ok(huaweiHealthAuthFacade.authorizeUrl(subjectId));
    }

    /**
     * 用授权码换票并绑定「我的」主体。
     *
     * @param dto 回调参数
     * @return 授权摘要
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "完成我的华为 OAuth 回调")
    @MiyfPermission(code = "health:user:huawei:oauth")
    @PostMapping("/oauth/callback")
    public ApiResult<Map<String, Object>> oauthCallback(@Valid @RequestBody HuaweiOAuthCallbackDto dto) {
        Long mine = healthCrudApplicationService.getOrCreateMySubjectEntity().getId();
        Long requested = parseOptionalSubjectId(dto.getSubjectId());
        if (requested != null && !requested.equals(mine)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能绑定自己的健康主体");
        }
        return ApiResult.ok(huaweiHealthAuthFacade.completeOAuth(mine, dto.getCode(), dto.getState()));
    }

    /**
     * 查询「我的」华为授权状态。
     *
     * @return 状态摘要
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "我的华为授权状态")
    @MiyfPermission(code = "health:user:huawei:oauth")
    @GetMapping("/oauth/status")
    public ApiResult<Map<String, Object>> oauthStatus() {
        Long subjectId = healthCrudApplicationService.getOrCreateMySubjectEntity().getId();
        return ApiResult.ok(huaweiHealthAuthFacade.status(subjectId));
    }

    /**
     * 撤销「我的」华为授权。
     *
     * @return 空成功
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "撤销我的华为授权")
    @MiyfPermission(code = "health:user:huawei:oauth")
    @DeleteMapping("/oauth")
    public ApiResult<Void> revoke() {
        Long subjectId = healthCrudApplicationService.getOrCreateMySubjectEntity().getId();
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
