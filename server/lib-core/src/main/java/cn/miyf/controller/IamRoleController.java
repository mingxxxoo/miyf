package cn.miyf.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.bean.dto.SysRoleSaveDto;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.service.IamApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.RequirePermission;
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
 * 角色管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-角色")
@RestController
@RequestMapping("/api/iam/roles")
public class IamRoleController {

    private final IamApplicationService iamApplicationService;

    public IamRoleController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    @Operation(summary = "角色列表")
    @MiyfPermission(code = "iam:role:list", name = "角色列表", groupCode = "iam_role", groupName = "角色管理")
    @RequirePermission({"iam:role:list"})
    @GetMapping
    public ApiResult<List<SysRoleEntity>> list() {
        return ApiResult.ok(iamApplicationService.listRoles());
    }

    @Operation(summary = "创建角色")
    @MiyfPermission(code = "iam:role:create", name = "创建角色", groupCode = "iam_role", groupName = "角色管理")
    @RequirePermission({"iam:role:create"})
    @PostMapping
    public ApiResult<SysRoleEntity> create(@Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(iamApplicationService.createRole(dto));
    }

    @Operation(summary = "更新角色")
    @MiyfPermission(code = "iam:role:update", name = "更新角色", groupCode = "iam_role", groupName = "角色管理")
    @RequirePermission({"iam:role:update"})
    @PutMapping("/{id}")
    public ApiResult<SysRoleEntity> update(@PathVariable Long id, @Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(iamApplicationService.updateRole(id, dto));
    }

    @Operation(summary = "删除角色")
    @MiyfPermission(code = "iam:role:delete", name = "删除角色", groupCode = "iam_role", groupName = "角色管理")
    @RequirePermission({"iam:role:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        iamApplicationService.deleteRole(id);
        return ApiResult.ok();
    }
}
