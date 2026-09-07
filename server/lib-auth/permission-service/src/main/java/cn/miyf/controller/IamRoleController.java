package cn.miyf.controller;

import cn.miyf.bean.dto.RoleDefaultDto;
import cn.miyf.bean.dto.RoleUsersAddDto;
import cn.miyf.bean.dto.SysRoleSaveDto;
import cn.miyf.bean.vo.RoleAuthSummaryVo;
import cn.miyf.bean.vo.RoleUserVo;
import cn.miyf.bean.vo.SysRoleVo;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.IamAdminPopedom;
import cn.miyf.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 角色管理与人员授权。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-角色")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/roles")
public class IamRoleController {

    private final PermissionApplicationService permissionApplicationService;

    public IamRoleController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "角色列表")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping
    public ApiResult<List<SysRoleVo>> list(@RequestParam(required = false) String product) {
        return ApiResult.ok(permissionApplicationService.listRoles(product));
    }

    @Operation(summary = "人员授权汇总")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping("/auth-summary")
    public ApiResult<RoleAuthSummaryVo> authSummary(@RequestParam(required = false) String product) {
        return ApiResult.ok(permissionApplicationService.authSummary(product));
    }

    @Operation(summary = "创建角色")
    @MiyfPermission(code = "iam:role:create")
    @PostMapping
    public ApiResult<SysRoleVo> create(@Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createRole(dto));
    }

    @Operation(summary = "更新角色")
    @MiyfPermission(code = "iam:role:update")
    @PutMapping("/{id}")
    public ApiResult<SysRoleVo> update(@PathVariable Long id, @Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updateRole(id, dto));
    }

    @Operation(summary = "设/取消默认角色")
    @MiyfPermission(code = "iam:role:update")
    @PutMapping("/{id}/default")
    public ApiResult<SysRoleVo> setDefault(@PathVariable Long id, @RequestBody RoleDefaultDto dto) {
        return ApiResult.ok(permissionApplicationService.setDefaultRole(id, dto));
    }

    @Operation(summary = "删除角色")
    @MiyfPermission(code = "iam:role:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deleteRole(id);
        return ApiResult.ok();
    }

    @Operation(summary = "角色下用户")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping("/{id}/users")
    public ApiResult<List<RoleUserVo>> listUsers(@PathVariable Long id) {
        return ApiResult.ok(permissionApplicationService.listRoleUsers(id));
    }

    @Operation(summary = "角色添加人员")
    @MiyfPermission(code = "iam:user:update")
    @PostMapping("/{id}/users")
    public ApiResult<Void> addUsers(@PathVariable Long id, @Valid @RequestBody RoleUsersAddDto dto) {
        permissionApplicationService.addRoleUsers(id, dto);
        return ApiResult.ok();
    }

    @Operation(summary = "角色移除人员")
    @MiyfPermission(code = "iam:user:update")
    @DeleteMapping("/{id}/users/{userId}")
    public ApiResult<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        permissionApplicationService.removeRoleUser(id, userId);
        return ApiResult.ok();
    }

    @Operation(summary = "导出角色授权 CSV")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping("/{id}/users/export")
    public ResponseEntity<byte[]> exportUsers(@PathVariable Long id) {
        byte[] body = permissionApplicationService.exportRoleUsersCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"role-" + id + "-users.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
