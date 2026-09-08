package cn.miyf.user.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.permission.bean.vo.SysMenuTreeVo;
import cn.miyf.permission.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前登录管理员的 IAM 自助接口。
 * 菜单树按登录用户权限裁剪，供管理端侧栏渲染。
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

    /**
     * 构造控制器。
     *
     * @param permissionApplicationService 权限应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public IamMeController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    /**
     * 获取当前登录用户可见的菜单树。
     *
     * @return 菜单树节点列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "当前用户菜单树")
    @GetMapping("/menus")
    public ApiResult<List<SysMenuTreeVo>> menus() {
        return ApiResult.ok(permissionApplicationService.currentUserMenus());
    }
}
