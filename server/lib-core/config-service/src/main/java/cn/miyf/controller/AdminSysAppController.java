package cn.miyf.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.RequirePermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.bean.dto.SysAppSaveDto;
import cn.miyf.bean.entity.SysAppEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.service.SystemAppApplicationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统应用管理接口。
 * 维护产品域（sys_app），供菜单/角色按 product 对齐。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Tag(name = "系统-应用")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system/apps")
public class AdminSysAppController {

    private final SystemAppApplicationService systemAppApplicationService;

    /**
     * 构造控制器。
     *
     * @param systemAppApplicationService 应用服务
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    public AdminSysAppController(SystemAppApplicationService systemAppApplicationService) {
        this.systemAppApplicationService = systemAppApplicationService;
    }

    /**
     * 应用列表，支持关键字与状态过滤。
     * 列表权限允许与角色列表权限 OR 匹配，便于授权页下拉。
     *
     * @param keyword 可选关键字
     * @param status  可选状态
     * @return 应用列表
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Operation(summary = "应用列表")
    @MiyfPermission(code = "sys:app:list")
    @RequirePermission({"sys:app:list", "iam:role:list"})
    @GetMapping
    public ApiResult<List<SysAppEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(systemAppApplicationService.list(keyword, status));
    }

    /**
     * 创建系统应用。
     *
     * @param dto 保存请求
     * @return 新建应用
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Operation(summary = "创建应用")
    @MiyfPermission(code = "sys:app:create")
    @PostMapping
    public ApiResult<SysAppEntity> create(@Valid @RequestBody SysAppSaveDto dto) {
        return ApiResult.ok(systemAppApplicationService.create(dto));
    }

    /**
     * 更新系统应用。
     *
     * @param id  应用 ID
     * @param dto 保存请求
     * @return 更新后应用
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Operation(summary = "更新应用")
    @MiyfPermission(code = "sys:app:update")
    @PutMapping("/{id}")
    public ApiResult<SysAppEntity> update(@PathVariable Long id, @Valid @RequestBody SysAppSaveDto dto) {
        return ApiResult.ok(systemAppApplicationService.update(id, dto));
    }

    /**
     * 删除系统应用。
     *
     * @param id 应用 ID
     * @return 空成功结果
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Operation(summary = "删除应用")
    @MiyfPermission(code = "sys:app:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        systemAppApplicationService.delete(id);
        return ApiResult.ok();
    }
}
