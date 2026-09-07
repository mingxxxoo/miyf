package cn.miyf.controller;

import cn.miyf.bean.dto.PermGroupSaveDto;
import cn.miyf.bean.vo.SysPermGroupVo;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.IamAdminPopedom;
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
 * 权限组管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-权限组")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/perm-groups")
public class IamPermGroupController {

    private final PermissionApplicationService permissionApplicationService;

    public IamPermGroupController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "权限组列表")
    @MiyfPermission(code = "iam:perm-group:list")
    @GetMapping
    public ApiResult<List<SysPermGroupVo>> list() {
        return ApiResult.ok(permissionApplicationService.listPermGroups());
    }

    @Operation(summary = "创建权限组")
    @MiyfPermission(code = "iam:perm-group:create")
    @PostMapping
    public ApiResult<SysPermGroupVo> create(@Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createPermGroup(dto));
    }

    @Operation(summary = "更新权限组")
    @MiyfPermission(code = "iam:perm-group:update")
    @PutMapping("/{id}")
    public ApiResult<SysPermGroupVo> update(@PathVariable Long id, @Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updatePermGroup(id, dto));
    }

    @Operation(summary = "删除权限组")
    @MiyfPermission(code = "iam:perm-group:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deletePermGroup(id);
        return ApiResult.ok();
    }
}
