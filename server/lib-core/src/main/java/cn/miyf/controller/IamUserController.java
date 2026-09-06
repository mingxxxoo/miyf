package cn.miyf.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.bean.dto.SysUserSaveDto;
import cn.miyf.bean.entity.SysUserEntity;
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
 * 系统用户管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-用户")
@RestController
@RequestMapping("/api/iam/users")
public class IamUserController {

    private final IamApplicationService iamApplicationService;

    /**
     * 构造控制器。
     *
     * @param iamApplicationService IAM 服务
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public IamUserController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    /**
     * 用户列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "用户列表")
    @MiyfPermission(code = "iam:user:list", name = "用户列表", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:list"})
    @GetMapping
    public ApiResult<List<SysUserEntity>> list() {
        return ApiResult.ok(iamApplicationService.listUsers());
    }

    /**
     * 创建用户。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "创建用户")
    @MiyfPermission(code = "iam:user:create", name = "创建用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:create"})
    @PostMapping
    public ApiResult<SysUserEntity> create(@Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(iamApplicationService.createUser(dto));
    }

    /**
     * 更新用户。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "更新用户")
    @MiyfPermission(code = "iam:user:update", name = "更新用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:update"})
    @PutMapping("/{id}")
    public ApiResult<SysUserEntity> update(@PathVariable Long id, @Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(iamApplicationService.updateUser(id, dto));
    }

    /**
     * 删除用户。
     *
     * @param id ID
     * @return 空
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "删除用户")
    @MiyfPermission(code = "iam:user:delete", name = "删除用户", groupCode = "iam_user", groupName = "人员管理")
    @RequirePermission({"iam:user:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        iamApplicationService.deleteUser(id);
        return ApiResult.ok();
    }
}
