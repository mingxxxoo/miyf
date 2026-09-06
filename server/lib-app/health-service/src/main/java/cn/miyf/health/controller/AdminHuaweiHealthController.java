package cn.miyf.health.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.health.provider.huawei.HuaweiHealthAuthFacade;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 华为 Health Kit：仅支持 OAuth 授权（按健康主体独立绑定）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "健康-华为授权")
@PopedomGroup(value = "12010000", name = "个人", product = "health", sort = 20)
@RestController
@RequestMapping("/api/admin/health/providers/huawei")
public class AdminHuaweiHealthController {

    private final HuaweiHealthAuthFacade huaweiHealthAuthFacade;

    public AdminHuaweiHealthController(HuaweiHealthAuthFacade huaweiHealthAuthFacade) {
        this.huaweiHealthAuthFacade = huaweiHealthAuthFacade;
    }

    @Operation(summary = "获取华为 OAuth 授权 URL（绑定到指定主体）")
    @MiyfPermission(code = "health:huawei:oauth", name = "华为授权", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:huawei:oauth"})
    @GetMapping("/authorize-url")
    public ApiResult<Map<String, Object>> authorizeUrl(@RequestParam Long subjectId) {
        return ApiResult.ok(huaweiHealthAuthFacade.authorizeUrl(subjectId));
    }

    @Operation(summary = "用授权码换取 Token 并绑定主体")
    @MiyfPermission(code = "health:huawei:oauth", name = "华为授权", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:huawei:oauth"})
    @PostMapping("/oauth/callback")
    public ApiResult<Map<String, Object>> oauthCallback(@RequestBody Map<String, String> body) {
        Long subjectId = null;
        if (body.get("subjectId") != null && !body.get("subjectId").isBlank()) {
            subjectId = Long.parseLong(body.get("subjectId").trim());
        }
        return ApiResult.ok(huaweiHealthAuthFacade.completeOAuth(
                subjectId,
                body.get("code"),
                body.get("state")));
    }

    @Operation(summary = "查询主体华为授权状态")
    @MiyfPermission(code = "health:huawei:oauth", name = "华为授权", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:huawei:oauth"})
    @GetMapping("/oauth/status")
    public ApiResult<Map<String, Object>> oauthStatus(@RequestParam Long subjectId) {
        return ApiResult.ok(huaweiHealthAuthFacade.status(subjectId));
    }

    @Operation(summary = "撤销指定主体的华为授权")
    @MiyfPermission(code = "health:huawei:oauth", name = "华为授权", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:huawei:oauth"})
    @DeleteMapping("/oauth")
    public ApiResult<Void> revoke(@RequestParam Long subjectId) {
        huaweiHealthAuthFacade.revoke(subjectId);
        return ApiResult.ok();
    }
}
