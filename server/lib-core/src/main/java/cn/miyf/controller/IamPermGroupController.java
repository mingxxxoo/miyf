package cn.miyf.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.bean.dto.PermGroupSaveDto;
import cn.miyf.bean.entity.SysPermGroupEntity;
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
 * 权限组管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-权限组")
@RestController
@RequestMapping("/api/iam/perm-groups")
public class IamPermGroupController {

    private final IamApplicationService iamApplicationService;

    public IamPermGroupController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    @Operation(summary = "权限组列表")
    @MiyfPermission(code = "iam:perm-group:list", name = "权限组列表", groupCode = "iam_perm_group", groupName = "权限组")
    @RequirePermission({"iam:perm-group:list"})
    @GetMapping
    public ApiResult<List<SysPermGroupEntity>> list() {
        return ApiResult.ok(iamApplicationService.listPermGroups());
    }

    @Operation(summary = "创建权限组")
    @MiyfPermission(code = "iam:perm-group:create", name = "创建权限组", groupCode = "iam_perm_group", groupName = "权限组")
    @RequirePermission({"iam:perm-group:create"})
    @PostMapping
    public ApiResult<SysPermGroupEntity> create(@Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(iamApplicationService.createPermGroup(dto));
    }

    @Operation(summary = "更新权限组")
    @MiyfPermission(code = "iam:perm-group:update", name = "更新权限组", groupCode = "iam_perm_group", groupName = "权限组")
    @RequirePermission({"iam:perm-group:update"})
    @PutMapping("/{id}")
    public ApiResult<SysPermGroupEntity> update(@PathVariable Long id, @Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(iamApplicationService.updatePermGroup(id, dto));
    }

    @Operation(summary = "删除权限组")
    @MiyfPermission(code = "iam:perm-group:delete", name = "删除权限组", groupCode = "iam_perm_group", groupName = "权限组")
    @RequirePermission({"iam:perm-group:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        iamApplicationService.deletePermGroup(id);
        return ApiResult.ok();
    }
}
