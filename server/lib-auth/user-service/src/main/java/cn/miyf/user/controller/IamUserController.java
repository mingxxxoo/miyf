package cn.miyf.user.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.user.bean.dto.SysUserSaveDto;
import cn.miyf.user.bean.dto.SysUserStatusUpdateDto;
import cn.miyf.user.bean.vo.SysUserVo;
import cn.miyf.user.service.SysUserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 系统用户（sys_user）管理接口。
 * 列表与写操作受数据范围约束；密码哈希不对前端回传。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-用户")
@IamAdminPopedom
@RestController
@RequestMapping("/iam/users")
@RequiredArgsConstructor
public class IamUserController {

    private final SysUserApplicationService sysUserApplicationService;

    
    /**
     * 查询数据范围内的系统用户列表（脱敏 VO）。
     *
     * @return 用户 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "用户列表")
    @MiyfPermission(code = "iam:user:list")
    @GetMapping
    public ApiResult<List<SysUserVo>> list() {
        return ApiResult.ok(sysUserApplicationService.listUsers());
    }

    /**
     * 创建系统用户并可附带角色。
     *
     * @param dto 用户保存请求
     * @return 新建用户 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建用户")
    @MiyfPermission(code = "iam:user:create")
    @PostMapping
    public ApiResult<SysUserVo> create(@Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(sysUserApplicationService.createUser(dto));
    }

    /**
     * 更新系统用户基本信息与角色绑定。
     *
     * @param id  用户 ID
     * @param dto 用户保存请求
     * @return 更新后用户 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新用户")
    @MiyfPermission(code = "iam:user:update")
    @PutMapping("/{id}")
    public ApiResult<SysUserVo> update(@PathVariable Long id, @Valid @RequestBody SysUserSaveDto dto) {
        return ApiResult.ok(sysUserApplicationService.updateUser(id, dto));
    }

    /**
     * 仅启停登录，不改角色。
     *
     * @param id  用户 ID
     * @param dto 状态
     * @return VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "启停用户登录")
    @MiyfPermission(code = "iam:user:update")
    @PutMapping("/{id}/status")
    public ApiResult<SysUserVo> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody SysUserStatusUpdateDto dto) {
        return ApiResult.ok(sysUserApplicationService.updateUserStatus(id, dto.getStatus()));
    }

    /**
     * 删除系统用户及其角色关联。
     *
     * @param id 用户 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除用户")
    @MiyfPermission(code = "iam:user:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysUserApplicationService.deleteUser(id);
        return ApiResult.ok();
    }
}
