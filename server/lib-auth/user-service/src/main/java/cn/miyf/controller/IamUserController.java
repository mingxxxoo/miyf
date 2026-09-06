package cn.miyf.controller;

import cn.miyf.bean.dto.SysUserSaveDto;
import cn.miyf.bean.vo.SysUserVo;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import cn.miyf.service.SysUserApplicationService;
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
 * 系统用户管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-用户")
@PopedomGroup(value = "10030000", name = "管理员", product = "iam", sort = 5)
@RestController
@RequestMapping("/api/iam/users")
public class IamUserController {

    private final SysUserApplicationService sysUserApplicationService;

    public IamUserController(SysUserApplicationService sysUserApplicationService) {
        this.sysUserApplicationService = sysUserApplicationService;
    }

    @Operation(summary = "用户列表")
    @MiyfPermission(code = "iam:user:list", name = "用户列表", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:list"})
    @GetMapping
    public ApiResult<List<SysUserVo>> list() {
        return ApiResult.ok(sysUserApplicationService.listUsers());
    }

    @Operation(summary = "创建用户")
    @MiyfPermission(code = "iam:user:create", name = "创建用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:create"})
    @PostMapping
    public ApiResult<SysUserVo> create(@Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(sysUserApplicationService.createUser(dto));
    }

    @Operation(summary = "更新用户")
    @MiyfPermission(code = "iam:user:update", name = "更新用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:update"})
    @PutMapping("/{id}")
    public ApiResult<SysUserVo> update(@PathVariable Long id, @Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(sysUserApplicationService.updateUser(id, dto));
    }

    @Operation(summary = "删除用户")
    @MiyfPermission(code = "iam:user:delete", name = "删除用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysUserApplicationService.deleteUser(id);
        return ApiResult.ok();
    }
}
