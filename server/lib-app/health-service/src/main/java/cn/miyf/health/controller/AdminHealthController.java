package cn.miyf.health.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.bean.dto.HealthSampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSubjectEntity;
import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.bean.vo.HealthOverviewVo;
import cn.miyf.health.bean.vo.HealthProviderVo;
import cn.miyf.health.bean.vo.HealthTrendVo;
import cn.miyf.health.service.HealthApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 健康管理 API：主体、采样、数据源与同步。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "健康管理")
@PopedomGroup(value = "12010000", name = "个人", product = "health", sort = 20)
@RestController
@RequestMapping("/api/admin/health")
public class AdminHealthController {

    private final HealthApplicationService healthApplicationService;

    public AdminHealthController(HealthApplicationService healthApplicationService) {
        this.healthApplicationService = healthApplicationService;
    }

    @Operation(summary = "健康概览")
    @MiyfPermission(code = "health:overview:view", name = "健康概览", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:overview:view"})
    @GetMapping("/overview")
    public ApiResult<HealthOverviewVo> overview() {
        return ApiResult.ok(healthApplicationService.overview());
    }

    @Operation(summary = "数据源列表")
    @MiyfPermission(code = "health:provider:list", name = "数据源列表", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:provider:list"})
    @GetMapping("/providers")
    public ApiResult<List<HealthProviderVo>> providers() {
        return ApiResult.ok(healthApplicationService.listProviders());
    }

    @Operation(summary = "触发数据源同步")
    @MiyfPermission(code = "health:sync:trigger", name = "触发同步", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sync:trigger"})
    @PostMapping("/providers/{providerCode}/sync")
    public ApiResult<HealthSyncRunEntity> sync(@PathVariable String providerCode,
                                               @Valid @RequestBody HealthSyncRequestDto dto) {
        return ApiResult.ok(healthApplicationService.sync(providerCode, dto));
    }

    @Operation(summary = "同步运行记录")
    @MiyfPermission(code = "health:sync:list", name = "同步记录", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sync:list"})
    @GetMapping("/sync-runs")
    public ApiResult<List<HealthSyncRunEntity>> listSyncRuns(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthApplicationService.listSyncRuns(providerCode, subjectId, limit));
    }

    @Operation(summary = "健康主体列表")
    @MiyfPermission(code = "health:subject:list", name = "主体列表", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:subject:list"})
    @GetMapping("/subjects")
    public ApiResult<List<HealthSubjectEntity>> listSubjects(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(healthApplicationService.listSubjects(keyword));
    }

    @Operation(summary = "创建健康主体")
    @MiyfPermission(code = "health:subject:create", name = "创建主体", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:subject:create"})
    @PostMapping("/subjects")
    public ApiResult<HealthSubjectEntity> createSubject(@Valid @RequestBody HealthSubjectSaveDto dto) {
        return ApiResult.ok(healthApplicationService.createSubject(dto));
    }

    @Operation(summary = "更新健康主体")
    @MiyfPermission(code = "health:subject:update", name = "更新主体", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:subject:update"})
    @PutMapping("/subjects/{id}")
    public ApiResult<HealthSubjectEntity> updateSubject(@PathVariable Long id,
                                                        @Valid @RequestBody HealthSubjectSaveDto dto) {
        return ApiResult.ok(healthApplicationService.updateSubject(id, dto));
    }

    @Operation(summary = "删除健康主体")
    @MiyfPermission(code = "health:subject:delete", name = "删除主体", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:subject:delete"})
    @DeleteMapping("/subjects/{id}")
    public ApiResult<Void> deleteSubject(@PathVariable Long id) {
        healthApplicationService.deleteSubject(id);
        return ApiResult.ok();
    }

    @Operation(summary = "主体数据源绑定列表")
    @MiyfPermission(code = "health:provider:list", name = "数据源列表", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:provider:list"})
    @GetMapping("/subjects/{subjectId}/bindings")
    public ApiResult<List<HealthProviderBindingEntity>> listBindings(@PathVariable Long subjectId) {
        return ApiResult.ok(healthApplicationService.listBindings(subjectId));
    }

    @Operation(summary = "绑定/更新数据源账号")
    @MiyfPermission(code = "health:sync:trigger", name = "触发同步", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sync:trigger"})
    @PutMapping("/subjects/{subjectId}/bindings")
    public ApiResult<HealthProviderBindingEntity> upsertBinding(
            @PathVariable Long subjectId,
            @Valid @RequestBody HealthProviderBindingSaveDto dto) {
        return ApiResult.ok(healthApplicationService.upsertBinding(subjectId, dto));
    }

    @Operation(summary = "指标趋势")
    @MiyfPermission(code = "health:trend:view", name = "指标趋势", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:trend:view"})
    @GetMapping("/trends")
    public ApiResult<HealthTrendVo> trend(@RequestParam Long subjectId,
                                          @RequestParam String metricCode,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to,
                                          @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthApplicationService.trend(subjectId, metricCode, from, to, limit));
    }

    @Operation(summary = "采样列表")
    @MiyfPermission(code = "health:sample:list", name = "采样列表", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sample:list"})
    @GetMapping("/samples")
    public ApiResult<List<HealthSampleEntity>> listSamples(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String metricCode,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthApplicationService.listSamples(subjectId, metricCode, limit));
    }

    @Operation(summary = "手动录入采样")
    @MiyfPermission(code = "health:sample:create", name = "录入采样", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sample:create"})
    @PostMapping("/samples")
    public ApiResult<HealthSampleEntity> createSample(@Valid @RequestBody HealthSampleSaveDto dto) {
        return ApiResult.ok(healthApplicationService.createManualSample(dto));
    }

    @Operation(summary = "删除采样")
    @MiyfPermission(code = "health:sample:delete", name = "删除采样", groupCode = "12010000", groupName = "个人")
    @RequirePermission({"health:sample:delete"})
    @DeleteMapping("/samples/{id}")
    public ApiResult<Void> deleteSample(@PathVariable Long id) {
        healthApplicationService.deleteSample(id);
        return ApiResult.ok();
    }
}
