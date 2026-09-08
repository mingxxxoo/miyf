package cn.miyf.permission.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.permission.bean.vo.SysPermissionTreeVo;
import cn.miyf.permission.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限点查询接口。
 * 权限点由启动期 Bootstrap 扫描注解写入，本接口返回无限极权限树。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-权限")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/permissions")
public class IamPermissionController {

    private final PermissionApplicationService permissionApplicationService;

    /**
     * 构造控制器。
     *
     * @param permissionApplicationService 权限应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public IamPermissionController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    /**
     * 查询权限点无限极树（服务端 TreeUtils 组树）。
     *
     * @return 权限树根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "权限树")
    @MiyfPermission(code = "iam:permission:list")
    @GetMapping
    public ApiResult<List<SysPermissionTreeVo>> list() {
        return ApiResult.ok(permissionApplicationService.listPermissionsTree());
    }
}
