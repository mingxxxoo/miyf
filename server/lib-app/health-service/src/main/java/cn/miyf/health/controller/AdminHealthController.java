package cn.miyf.health.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.bean.dto.HealthSampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.vo.HealthOverviewVo;
import cn.miyf.health.bean.vo.HealthProviderBindingVo;
import cn.miyf.health.bean.vo.HealthProviderVo;
import cn.miyf.health.bean.vo.HealthSampleVo;
import cn.miyf.health.bean.vo.HealthSubjectVo;
import cn.miyf.health.bean.vo.HealthSyncRunVo;
import cn.miyf.health.bean.vo.HealthTrendVo;
import cn.miyf.health.security.HealthPersonalPopedom;
import cn.miyf.health.service.HealthCrudApplicationService;
import cn.miyf.health.service.HealthSyncApplicationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 健康管理 API：主体、采样、数据源绑定与同步编排入口。
 * 返回统一使用 VO；主体相关读写受 DataScope 约束。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "健康管理")
@HealthPersonalPopedom
@RestController
@RequestMapping("/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {

    private final HealthCrudApplicationService healthCrudApplicationService;
    private final HealthSyncApplicationService healthSyncApplicationService;

    /**
     * 查询健康模块概览（启用状态、主体/采样数量、数据源列表）。
     *
     * @return 概览 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "健康概览")
    @MiyfPermission(code = "health:overview:view")
    @GetMapping("/overview")
    public ApiResult<HealthOverviewVo> overview() {
        return ApiResult.ok(healthCrudApplicationService.overview());
    }

    /**
     * 列出已注册的健康数据源（含是否启用、是否支持远程拉取）。
     *
     * @return 数据源 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "数据源列表")
    @MiyfPermission(code = "health:provider:list")
    @GetMapping("/providers")
    public ApiResult<List<HealthProviderVo>> providers() {
        return ApiResult.ok(healthCrudApplicationService.listProviders());
    }

    /**
     * 触发指定数据源对某主体的远程同步并入库。
     *
     * @param providerCode 数据源编码
     * @param dto          同步请求（主体、时间窗、指标等）
     * @return 同步运行记录 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "触发数据源同步")
    @MiyfPermission(code = "health:sync:trigger")
    @PostMapping("/providers/{providerCode}/sync")
    public ApiResult<HealthSyncRunVo> sync(@PathVariable String providerCode,
                                           @Valid @RequestBody HealthSyncRequestDto dto) {
        return ApiResult.ok(healthSyncApplicationService.syncAsVo(providerCode, dto));
    }

    /**
     * 查询同步运行历史，可按数据源与主体过滤。
     *
     * @param providerCode 可选数据源编码
     * @param subjectId    可选主体 ID
     * @param limit        条数上限
     * @return 运行记录列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "同步运行记录")
    @MiyfPermission(code = "health:sync:list")
    @GetMapping("/sync-runs")
    public ApiResult<List<HealthSyncRunVo>> listSyncRuns(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthSyncApplicationService.listSyncRuns(providerCode, subjectId, limit));
    }

    /**
     * 健康主体列表，支持关键字模糊匹配。
     *
     * @param keyword 可选关键字
     * @return 主体列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "健康主体列表")
    @MiyfPermission(code = "health:subject:list")
    @GetMapping("/subjects")
    public ApiResult<List<HealthSubjectVo>> listSubjects(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(healthCrudApplicationService.listSubjects(keyword));
    }

    /**
     * 创建健康主体。
     *
     * @param dto 主体保存请求
     * @return 新建主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建健康主体")
    @MiyfPermission(code = "health:subject:create")
    @PostMapping("/subjects")
    public ApiResult<HealthSubjectVo> createSubject(@Valid @RequestBody HealthSubjectSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.createSubject(dto));
    }

    /**
     * 更新健康主体。
     *
     * @param id  主体 ID
     * @param dto 主体保存请求
     * @return 更新后主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新健康主体")
    @MiyfPermission(code = "health:subject:update")
    @PutMapping("/subjects/{id}")
    public ApiResult<HealthSubjectVo> updateSubject(@PathVariable Long id,
                                                    @Valid @RequestBody HealthSubjectSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.updateSubject(id, dto));
    }

    /**
     * 删除健康主体，并清理绑定与厂商授权凭证。
     *
     * @param id 主体 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除健康主体")
    @MiyfPermission(code = "health:subject:delete")
    @DeleteMapping("/subjects/{id}")
    public ApiResult<Void> deleteSubject(@PathVariable Long id) {
        healthCrudApplicationService.deleteSubject(id);
        return ApiResult.ok();
    }

    /**
     * 列出某主体已绑定的数据源账号（脱敏，不含 credentialRef）。
     *
     * @param subjectId 主体 ID
     * @return 绑定列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "主体数据源绑定列表")
    @MiyfPermission(code = "health:provider:list")
    @GetMapping("/subjects/{subjectId}/bindings")
    public ApiResult<List<HealthProviderBindingVo>> listBindings(@PathVariable Long subjectId) {
        return ApiResult.ok(healthCrudApplicationService.listBindings(subjectId));
    }

    /**
     * 创建或更新主体与数据源账号的绑定关系。
     *
     * @param subjectId 主体 ID
     * @param dto       绑定保存请求
     * @return 绑定 VO（脱敏）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "绑定/更新数据源账号")
    @MiyfPermission(code = "health:provider:list")
    @PutMapping("/subjects/{subjectId}/bindings")
    public ApiResult<HealthProviderBindingVo> upsertBinding(
            @PathVariable Long subjectId,
            @Valid @RequestBody HealthProviderBindingSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.upsertBinding(subjectId, dto));
    }

    /**
     * 查询指定主体某指标的时间序列趋势（含最值、均值等汇总）。
     *
     * @param subjectId  主体 ID
     * @param metricCode 指标编码
     * @param from       可选起始时间
     * @param to         可选结束时间
     * @param limit      可选点数上限
     * @return 趋势 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "指标趋势")
    @MiyfPermission(code = "health:trend:view")
    @GetMapping("/trends")
    public ApiResult<HealthTrendVo> trend(@RequestParam Long subjectId,
                                          @RequestParam String metricCode,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to,
                                          @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthCrudApplicationService.trend(subjectId, metricCode, from, to, limit));
    }

    /**
     * 采样点列表，可按主体与指标过滤。
     *
     * @param subjectId  可选主体 ID
     * @param metricCode 可选指标编码
     * @param limit      可选条数上限
     * @return 采样列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "采样列表")
    @MiyfPermission(code = "health:sample:list")
    @GetMapping("/samples")
    public ApiResult<List<HealthSampleVo>> listSamples(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String metricCode,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthCrudApplicationService.listSamples(subjectId, metricCode, limit));
    }

    /**
     * 手动录入一条采样（数据源为 MANUAL）。
     *
     * @param dto 采样保存请求
     * @return 新建采样
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "手动录入采样")
    @MiyfPermission(code = "health:sample:create")
    @PostMapping("/samples")
    public ApiResult<HealthSampleVo> createSample(@Valid @RequestBody HealthSampleSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.createManualSample(dto));
    }

    /**
     * 按 ID 删除采样点。
     *
     * @param id 采样 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除采样")
    @MiyfPermission(code = "health:sample:delete")
    @DeleteMapping("/samples/{id}")
    public ApiResult<Void> deleteSample(@PathVariable Long id) {
        healthCrudApplicationService.deleteSample(id);
        return ApiResult.ok();
    }
}
