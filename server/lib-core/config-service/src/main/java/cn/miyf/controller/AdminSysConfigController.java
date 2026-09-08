package cn.miyf.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.bean.dto.SysConfigSaveDto;
import cn.miyf.bean.entity.SysConfigEntity;
import cn.miyf.common.ApiResult;
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
 * 系统配置管理接口。
 * 按分组与关键字维护键值配置。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-配置")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system/configs")
public class AdminSysConfigController {

    private final SystemConfigApplicationService systemConfigApplicationService;

    /**
     * 构造控制器。
     *
     * @param systemConfigApplicationService 配置应用服务
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public AdminSysConfigController(SystemConfigApplicationService systemConfigApplicationService) {
        this.systemConfigApplicationService = systemConfigApplicationService;
    }

    /**
     * 配置列表，可按分组与关键字过滤。
     *
     * @param groupCode 可选分组编码
     * @param keyword   可选关键字
     * @return 配置列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "配置列表")
    @MiyfPermission(code = "sys:config:list")
    @GetMapping
    public ApiResult<List<SysConfigEntity>> list(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(systemConfigApplicationService.list(groupCode, keyword));
    }

    /**
     * 按配置键精确查询。
     *
     * @param configKey 配置键
     * @return 配置实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "按键查询配置")
    @MiyfPermission(code = "sys:config:list")
    @GetMapping("/key/{configKey}")
    public ApiResult<SysConfigEntity> getByKey(@PathVariable String configKey) {
        return ApiResult.ok(systemConfigApplicationService.getByKey(configKey));
    }

    /**
     * 创建配置项。
     *
     * @param dto 保存请求
     * @return 新建配置
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "创建配置")
    @MiyfPermission(code = "sys:config:create")
    @PostMapping
    public ApiResult<SysConfigEntity> create(@Valid @RequestBody SysConfigSaveDto dto) {
        return ApiResult.ok(systemConfigApplicationService.create(dto));
    }

    /**
     * 更新配置项。
     *
     * @param id  配置 ID
     * @param dto 保存请求
     * @return 更新后配置
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "更新配置")
    @MiyfPermission(code = "sys:config:update")
    @PutMapping("/{id}")
    public ApiResult<SysConfigEntity> update(@PathVariable Long id, @Valid @RequestBody SysConfigSaveDto dto) {
        return ApiResult.ok(systemConfigApplicationService.update(id, dto));
    }

    /**
     * 删除配置项。
     *
     * @param id 配置 ID
     * @return 空成功结果
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "删除配置")
    @MiyfPermission(code = "sys:config:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        systemConfigApplicationService.delete(id);
        return ApiResult.ok();
    }
}
