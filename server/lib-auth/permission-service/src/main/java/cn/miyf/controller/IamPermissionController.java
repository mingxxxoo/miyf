package cn.miyf.controller;

import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import cn.miyf.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限点查询。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-权限")
@PopedomGroup(value = "10030000", name = "管理员", product = "iam", sort = 5)
@RestController
@RequestMapping("/api/iam/permissions")
public class IamPermissionController {

    private final PermissionApplicationService permissionApplicationService;

    public IamPermissionController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "权限列表")
    @MiyfPermission(code = "iam:permission:list", name = "权限列表", groupCode = "iam_permission", groupName = "权限")
    @RequirePermission({"iam:permission:list"})
    @GetMapping
    public ApiResult<List<SysPermissionEntity>> list() {
        return ApiResult.ok(permissionApplicationService.listPermissions());
    }
}
