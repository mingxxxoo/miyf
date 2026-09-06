package cn.miyf.controller;

import cn.miyf.bean.dto.SysConfigSaveDto;
import cn.miyf.bean.entity.SysConfigEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import cn.miyf.service.SystemConfigApplicationService;
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
 * 系统配置管理。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-配置")
@PopedomGroup(value = "10040000", name = "系统设置", product = "system", sort = 8)
@RestController
@RequestMapping("/api/admin/system/configs")
public class AdminSysConfigController {

    private final SystemConfigApplicationService systemConfigApplicationService;

    public AdminSysConfigController(SystemConfigApplicationService systemConfigApplicationService) {
        this.systemConfigApplicationService = systemConfigApplicationService;
    }

    @Operation(summary = "配置列表")
    @MiyfPermission(code = "sys:config:list", name = "配置列表", groupCode = "sys_config", groupName = "系统配置")
    @RequirePermission({"sys:config:list"})
    @GetMapping
    public ApiResult<List<SysConfigEntity>> list(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(systemConfigApplicationService.list(groupCode, keyword));
    }

    @Operation(summary = "按键查询配置")
    @MiyfPermission(code = "sys:config:list", name = "配置列表", groupCode = "sys_config", groupName = "系统配置")
    @RequirePermission({"sys:config:list"})
    @GetMapping("/key/{configKey}")
    public ApiResult<SysConfigEntity> getByKey(@PathVariable String configKey) {
        return ApiResult.ok(systemConfigApplicationService.getByKey(configKey));
    }

    @Operation(summary = "创建配置")
    @MiyfPermission(code = "sys:config:create", name = "创建配置", groupCode = "sys_config", groupName = "系统配置")
    @RequirePermission({"sys:config:create"})
    @PostMapping
    public ApiResult<SysConfigEntity> create(@Valid @RequestBody SysConfigSaveDto dto) {
        return ApiResult.ok(systemConfigApplicationService.create(dto));
    }

    @Operation(summary = "更新配置")
    @MiyfPermission(code = "sys:config:update", name = "更新配置", groupCode = "sys_config", groupName = "系统配置")
    @RequirePermission({"sys:config:update"})
    @PutMapping("/{id}")
    public ApiResult<SysConfigEntity> update(@PathVariable Long id, @Valid @RequestBody SysConfigSaveDto dto) {
        return ApiResult.ok(systemConfigApplicationService.update(id, dto));
    }

    @Operation(summary = "删除配置")
    @MiyfPermission(code = "sys:config:delete", name = "删除配置", groupCode = "sys_config", groupName = "系统配置")
    @RequirePermission({"sys:config:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        systemConfigApplicationService.delete(id);
        return ApiResult.ok();
    }
}
