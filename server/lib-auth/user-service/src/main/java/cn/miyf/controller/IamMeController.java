package cn.miyf.controller;

import cn.miyf.bean.vo.SysMenuTreeVo;
import cn.miyf.security.IamAdminPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前登录用户 IAM 接口。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-当前用户")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/me")
public class IamMeController {

    private final PermissionApplicationService permissionApplicationService;

    public IamMeController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "当前用户菜单树")
    @GetMapping("/menus")
    public ApiResult<List<SysMenuTreeVo>> menus() {
        return ApiResult.ok(permissionApplicationService.currentUserMenus());
    }
}
