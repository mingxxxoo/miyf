package cn.miyf.health.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.health.provider.huawei.HuaweiHealthAuthFacade;
import cn.miyf.health.security.HealthPersonalPopedom;
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
 * 华为 Health Kit OAuth 授权接口。
 * 按健康主体独立绑定，主体之间不共用 Token。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "健康-华为授权")
@HealthPersonalPopedom
@RestController
@RequestMapping("/api/admin/health/providers/huawei")
public class AdminHuaweiHealthController {

    private final HuaweiHealthAuthFacade huaweiHealthAuthFacade;

    /**
     * 构造控制器。
     *
     * @param huaweiHealthAuthFacade 华为授权编排门面
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public AdminHuaweiHealthController(HuaweiHealthAuthFacade huaweiHealthAuthFacade) {
        this.huaweiHealthAuthFacade = huaweiHealthAuthFacade;
    }

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
     * body 需包含 code、state，以及可选 subjectId。
     *
     * @param body 回调参数
     * @return 授权结果摘要
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "用授权码换取 Token 并绑定主体")
    @MiyfPermission(code = "health:huawei:oauth")
    @PostMapping("/oauth/callback")
    public ApiResult<Map<String, Object>> oauthCallback(@RequestBody Map<String, String> body) {
        Long subjectId = null;
        // subjectId 允许缺省：部分回调仅靠 state 解析主体
        if (body.get("subjectId") != null && !body.get("subjectId").isBlank()) {
            subjectId = Long.parseLong(body.get("subjectId").trim());
        }
        return ApiResult.ok(huaweiHealthAuthFacade.completeOAuth(
                subjectId,
                body.get("code"),
                body.get("state")));
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
}
