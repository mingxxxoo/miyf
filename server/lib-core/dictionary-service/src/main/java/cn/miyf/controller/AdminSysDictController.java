package cn.miyf.controller;

import cn.miyf.bean.dto.SysDictItemSaveDto;
import cn.miyf.bean.dto.SysDictTypeSaveDto;
import cn.miyf.bean.entity.SysDictItemEntity;
import cn.miyf.bean.entity.SysDictTypeEntity;
import cn.miyf.common.ApiResult;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.SystemSettingsPopedom;
import cn.miyf.service.SystemDictApplicationService;
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
import java.util.Map;

/**
 * 数据字典管理。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-数据字典")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system")
public class AdminSysDictController {

    private final SystemDictApplicationService systemDictApplicationService;

    public AdminSysDictController(SystemDictApplicationService systemDictApplicationService) {
        this.systemDictApplicationService = systemDictApplicationService;
    }

    @Operation(summary = "系统设置入口权限（仅注册权限码）")
    @MiyfPermission(code = "sys:settings:view")
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(Map.of("module", "system", "features", List.of("config", "dict", "permissions")));
    }

    @Operation(summary = "字典类型列表")
    @MiyfPermission(code = "sys:dict:list")
    @GetMapping("/dict-types")
    public ApiResult<List<SysDictTypeEntity>> listTypes(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(systemDictApplicationService.listTypes(keyword));
    }

    @Operation(summary = "创建字典类型")
    @MiyfPermission(code = "sys:dict:create")
    @PostMapping("/dict-types")
    public ApiResult<SysDictTypeEntity> createType(@Valid @RequestBody SysDictTypeSaveDto dto) {
        return ApiResult.ok(systemDictApplicationService.createType(dto));
    }

    @Operation(summary = "更新字典类型")
    @MiyfPermission(code = "sys:dict:update")
    @PutMapping("/dict-types/{id}")
    public ApiResult<SysDictTypeEntity> updateType(@PathVariable Long id, @Valid @RequestBody SysDictTypeSaveDto dto) {
        return ApiResult.ok(systemDictApplicationService.updateType(id, dto));
    }

    @Operation(summary = "删除字典类型")
    @MiyfPermission(code = "sys:dict:delete")
    @DeleteMapping("/dict-types/{id}")
    public ApiResult<Void> deleteType(@PathVariable Long id) {
        systemDictApplicationService.deleteType(id);
        return ApiResult.ok();
    }

    @Operation(summary = "按类型 ID 查询字典项")
    @MiyfPermission(code = "sys:dict:list")
    @GetMapping("/dict-types/{typeId}/items")
    public ApiResult<List<SysDictItemEntity>> listItemsByTypeId(@PathVariable Long typeId) {
        return ApiResult.ok(systemDictApplicationService.listItemsByTypeId(typeId));
    }

    @Operation(summary = "按类型编码查询字典项")
    @MiyfPermission(code = "sys:dict:list")
    @GetMapping("/dicts/{typeCode}/items")
    public ApiResult<List<SysDictItemEntity>> listItemsByTypeCode(
            @PathVariable String typeCode,
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        return ApiResult.ok(systemDictApplicationService.listItemsByTypeCode(typeCode, enabledOnly));
    }

    @Operation(summary = "创建字典项")
    @MiyfPermission(code = "sys:dict:create")
    @PostMapping("/dict-items")
    public ApiResult<SysDictItemEntity> createItem(@Valid @RequestBody SysDictItemSaveDto dto) {
        return ApiResult.ok(systemDictApplicationService.createItem(dto));
    }

    @Operation(summary = "更新字典项")
    @MiyfPermission(code = "sys:dict:update")
    @PutMapping("/dict-items/{id}")
    public ApiResult<SysDictItemEntity> updateItem(@PathVariable Long id, @Valid @RequestBody SysDictItemSaveDto dto) {
        return ApiResult.ok(systemDictApplicationService.updateItem(id, dto));
    }

    @Operation(summary = "删除字典项")
    @MiyfPermission(code = "sys:dict:delete")
    @DeleteMapping("/dict-items/{id}")
    public ApiResult<Void> deleteItem(@PathVariable Long id) {
        systemDictApplicationService.deleteItem(id);
        return ApiResult.ok();
    }
}
