package cn.miyf.permission.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.permission.bean.dto.RoleDefaultDto;
import cn.miyf.permission.bean.dto.RoleUsersAddDto;
import cn.miyf.permission.bean.dto.SysRoleSaveDto;
import cn.miyf.permission.bean.vo.RoleAuthSummaryVo;
import cn.miyf.permission.bean.vo.RoleUserVo;
import cn.miyf.permission.bean.vo.SysRoleVo;
import cn.miyf.permission.service.PermissionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 角色管理与人员授权接口。
 * 支持按产品域过滤、默认角色切换、角色下用户维护及 CSV 导出。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-角色")
@IamAdminPopedom
@RestController
@RequestMapping("/iam/roles")
@RequiredArgsConstructor
public class IamRoleController {

    private final PermissionApplicationService permissionApplicationService;

    
    /**
     * 查询角色列表，可按产品域过滤。
     *
     * @param product 可选产品域
     * @return 角色 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "角色列表")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping
    public ApiResult<List<SysRoleVo>> list(@RequestParam(required = false) String product) {
        return ApiResult.ok(permissionApplicationService.listRoles(product));
    }

    /**
     * 人员授权汇总（各角色人数等），供授权页概览。
     *
     * @param product 可选产品域
     * @return 授权汇总 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "人员授权汇总")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping("/auth-summary")
    public ApiResult<RoleAuthSummaryVo> authSummary(@RequestParam(required = false) String product) {
        return ApiResult.ok(permissionApplicationService.authSummary(product));
    }

    /**
     * 创建角色并可绑定权限组。
     *
     * @param dto 角色保存请求
     * @return 新建角色 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建角色")
    @MiyfPermission(code = "iam:role:create")
    @PostMapping
    public ApiResult<SysRoleVo> create(@Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createRole(dto));
    }

    /**
     * 更新角色及其权限组绑定。
     *
     * @param id  角色 ID
     * @param dto 角色保存请求
     * @return 更新后角色 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新角色")
    @MiyfPermission(code = "iam:role:update")
    @PutMapping("/{id}")
    public ApiResult<SysRoleVo> update(@PathVariable Long id, @Valid @RequestBody SysRoleSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updateRole(id, dto));
    }

    /**
     * 设置或取消该角色为产品域默认角色。
     *
     * @param id  角色 ID
     * @param dto 默认角色开关
     * @return 更新后角色 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "设/取消默认角色")
    @MiyfPermission(code = "iam:role:update")
    @PutMapping("/{id}/default")
    public ApiResult<SysRoleVo> setDefault(@PathVariable Long id, @RequestBody RoleDefaultDto dto) {
        return ApiResult.ok(permissionApplicationService.setDefaultRole(id, dto));
    }

    /**
     * 删除角色及其权限组、用户关联。
     *
     * @param id 角色 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除角色")
    @MiyfPermission(code = "iam:role:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deleteRole(id);
        return ApiResult.ok();
    }

    /**
     * 查询角色下已授权用户（受数据范围过滤）。
     *
     * @param id 角色 ID
     * @return 角色用户 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "角色下用户")
    @MiyfPermission(code = "iam:role:list")
    @GetMapping("/{id}/users")
    public ApiResult<List<RoleUserVo>> listUsers(@PathVariable Long id) {
        return ApiResult.ok(permissionApplicationService.listRoleUsers(id));
    }

    /**
     * 为角色批量添加用户。
     *
     * @param id  角色 ID
     * @param dto 用户 ID 列表
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "角色添加人员")
    @MiyfPermission(code = "iam:user:update")
    @PostMapping("/{id}/users")
    public ApiResult<Void> addUsers(@PathVariable Long id, @Valid @RequestBody RoleUsersAddDto dto) {
        permissionApplicationService.addRoleUsers(id, dto);
        return ApiResult.ok();
    }

    /**
     * 从角色移除指定用户。
     *
     * @param id     角色 ID
     * @param userId 用户 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "角色移除人员")
    @MiyfPermission(code = "iam:user:update")
    @DeleteMapping("/{id}/users/{userId}")
    public ApiResult<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        permissionApplicationService.removeRoleUser(id, userId);
        return ApiResult.ok();
    }

    /**
     * 导出角色下用户授权明细为 CSV 文件。
     *
     * @param id 角色 ID
     * @return CSV 字节响应
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
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
