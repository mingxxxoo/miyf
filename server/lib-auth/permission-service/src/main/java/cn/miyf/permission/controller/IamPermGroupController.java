package cn.miyf.permission.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.permission.bean.dto.PermGroupSaveDto;
import cn.miyf.permission.bean.vo.SysPermGroupVo;
import cn.miyf.permission.service.PermissionApplicationService;
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
 * 权限组管理接口。
 * 权限组用于批量绑定权限点并授权给角色。
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

    /**
     * 构造控制器。
     *
     * @param permissionApplicationService 权限应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public IamPermGroupController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    /**
     * 查询权限组列表（含组内权限码摘要）。
     *
     * @return 权限组 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "权限组列表")
    @MiyfPermission(code = "iam:perm-group:list")
    @GetMapping
    public ApiResult<List<SysPermGroupVo>> list() {
        return ApiResult.ok(permissionApplicationService.listPermGroups());
    }

    /**
     * 创建权限组及其权限项。
     *
     * @param dto 权限组保存请求
     * @return 新建权限组 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建权限组")
    @MiyfPermission(code = "iam:perm-group:create")
    @PostMapping
    public ApiResult<SysPermGroupVo> create(@Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createPermGroup(dto));
    }

    /**
     * 更新权限组及其权限项。
     *
     * @param id  权限组 ID
     * @param dto 权限组保存请求
     * @return 更新后权限组 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新权限组")
    @MiyfPermission(code = "iam:perm-group:update")
    @PutMapping("/{id}")
    public ApiResult<SysPermGroupVo> update(@PathVariable Long id, @Valid @RequestBody PermGroupSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updatePermGroup(id, dto));
    }

    /**
     * 删除权限组及其关联项。
     *
     * @param id 权限组 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除权限组")
    @MiyfPermission(code = "iam:perm-group:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deletePermGroup(id);
        return ApiResult.ok();
    }
}
