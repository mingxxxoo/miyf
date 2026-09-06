package cn.miyf.controller;

import cn.miyf.bean.dto.SysMenuSaveDto;
import cn.miyf.bean.entity.SysMenuEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import cn.miyf.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-菜单")
@PopedomGroup(value = "10030000", name = "管理员", product = "iam", sort = 5)
@RestController
@RequestMapping("/api/iam/menus")
public class IamMenuController {

    private final PermissionApplicationService permissionApplicationService;

    public IamMenuController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "菜单列表")
    @MiyfPermission(code = "iam:menu:list", name = "菜单列表", groupCode = "iam_menu", groupName = "菜单")
    @RequirePermission({"iam:menu:list"})
    @GetMapping
    public ApiResult<List<SysMenuEntity>> list() {
        return ApiResult.ok(permissionApplicationService.listMenus());
    }

    @Operation(summary = "创建菜单")
    @MiyfPermission(code = "iam:menu:create", name = "创建菜单", groupCode = "iam_menu", groupName = "菜单")
    @RequirePermission({"iam:menu:create"})
    @PostMapping
    public ApiResult<SysMenuEntity> create(@Valid @RequestBody SysMenuSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createMenu(dto));
    }

    @Operation(summary = "更新菜单")
    @MiyfPermission(code = "iam:menu:update", name = "更新菜单", groupCode = "iam_menu", groupName = "菜单")
    @RequirePermission({"iam:menu:update"})
    @PutMapping("/{id}")
    public ApiResult<SysMenuEntity> update(@PathVariable Long id, @Valid @RequestBody SysMenuSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updateMenu(id, dto));
    }

    @Operation(summary = "删除菜单")
    @MiyfPermission(code = "iam:menu:delete", name = "删除菜单", groupCode = "iam_menu", groupName = "菜单")
    @RequirePermission({"iam:menu:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deleteMenu(id);
        return ApiResult.ok();
    }
}
