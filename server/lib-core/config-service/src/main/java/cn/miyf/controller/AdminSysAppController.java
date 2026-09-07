package cn.miyf.controller;

import cn.miyf.bean.dto.SysAppSaveDto;
import cn.miyf.bean.entity.SysAppEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.SystemSettingsPopedom;
import cn.miyf.security.RequirePermission;
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
 * 系统应用管理。
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

    public AdminSysAppController(SystemAppApplicationService systemAppApplicationService) {
        this.systemAppApplicationService = systemAppApplicationService;
    }

    @Operation(summary = "应用列表")
    @MiyfPermission(code = "sys:app:list")
    @RequirePermission({"sys:app:list", "iam:role:list"})
    @GetMapping
    public ApiResult<List<SysAppEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(systemAppApplicationService.list(keyword, status));
    }

    @Operation(summary = "创建应用")
    @MiyfPermission(code = "sys:app:create")
    @PostMapping
    public ApiResult<SysAppEntity> create(@Valid @RequestBody SysAppSaveDto dto) {
        return ApiResult.ok(systemAppApplicationService.create(dto));
    }

    @Operation(summary = "更新应用")
    @MiyfPermission(code = "sys:app:update")
    @PutMapping("/{id}")
    public ApiResult<SysAppEntity> update(@PathVariable Long id, @Valid @RequestBody SysAppSaveDto dto) {
        return ApiResult.ok(systemAppApplicationService.update(id, dto));
    }

    @Operation(summary = "删除应用")
    @MiyfPermission(code = "sys:app:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        systemAppApplicationService.delete(id);
        return ApiResult.ok();
    }
}
