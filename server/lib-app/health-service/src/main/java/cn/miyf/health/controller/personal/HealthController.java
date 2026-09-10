package cn.miyf.health.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.health.bean.dto.HealthMySampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
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
 * 健康个人端 API：仅查询与处理当前登录用户自己的健康数据（微信小程序）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 */
@Tag(name = "用户端-健康")
@HealthPersonalPopedom
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCrudApplicationService healthCrudApplicationService;
    private final HealthSyncApplicationService healthSyncApplicationService;

    /**
     * 获取或自动创建「我的」健康主体。
     *
     * @return 主体 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "我的健康主体")
    @MiyfPermission(code = "health:user:me:view")
    @GetMapping("/me")
    public ApiResult<HealthSubjectVo> me() {
        return ApiResult.ok(healthCrudApplicationService.getOrCreateMySubject());
    }

    /**
     * 更新「我的」健康主体资料。
     *
     * @param dto 保存请求
     * @return 更新后主体
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "更新我的健康主体")
    @MiyfPermission(code = "health:user:me:update")
    @PutMapping("/me")
    public ApiResult<HealthSubjectVo> updateMe(@Valid @RequestBody HealthSubjectSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.updateMySubject(dto));
    }

    /**
     * 可用数据源列表。
     *
     * @return 数据源 VO 列表
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "数据源列表")
    @MiyfPermission(code = "health:user:provider:list")
    @GetMapping("/providers")
    public ApiResult<List<HealthProviderVo>> providers() {
        return ApiResult.ok(healthCrudApplicationService.listProviders());
    }

    /**
     * 我的数据源绑定。
     *
     * @return 绑定列表
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "我的数据源绑定")
    @MiyfPermission(code = "health:user:provider:list")
    @GetMapping("/me/bindings")
    public ApiResult<List<HealthProviderBindingVo>> myBindings() {
        return ApiResult.ok(healthCrudApplicationService.listMyBindings());
    }

    /**
     * 触发对我自己的远程同步。
     *
     * @param providerCode 数据源编码
     * @param dto          同步请求（subjectId 可空，服务端强制为自己）
     * @return 同步运行记录
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "同步我的健康数据")
    @MiyfPermission(code = "health:user:sync:trigger")
    @PostMapping("/providers/{providerCode}/sync")
    public ApiResult<HealthSyncRunVo> syncMine(@PathVariable String providerCode,
                                               @RequestBody(required = false) HealthSyncRequestDto dto) {
        return ApiResult.ok(healthSyncApplicationService.syncMineAsVo(providerCode, dto));
    }

    /**
     * 我的采样列表。
     *
     * @param metricCode 可选指标
     * @param limit      条数上限
     * @return 采样列表
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "我的采样列表")
    @MiyfPermission(code = "health:user:sample:list")
    @GetMapping("/me/samples")
    public ApiResult<List<HealthSampleVo>> mySamples(
            @RequestParam(required = false) String metricCode,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthCrudApplicationService.listMySamples(metricCode, limit));
    }

    /**
     * 为我手动录入一条采样。
     *
     * @param dto 采样请求
     * @return 新建采样
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "录入我的采样")
    @MiyfPermission(code = "health:user:sample:create")
    @PostMapping("/me/samples")
    public ApiResult<HealthSampleVo> createMySample(@Valid @RequestBody HealthMySampleSaveDto dto) {
        return ApiResult.ok(healthCrudApplicationService.createMyManualSample(dto));
    }

    /**
     * 删除我的采样。
     *
     * @param id 采样 ID
     * @return 空成功
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "删除我的采样")
    @MiyfPermission(code = "health:user:sample:delete")
    @DeleteMapping("/me/samples/{id}")
    public ApiResult<Void> deleteMySample(@PathVariable Long id) {
        healthCrudApplicationService.deleteMySample(id);
        return ApiResult.ok();
    }

    /**
     * 我的指标趋势。
     *
     * @param metricCode 指标编码
     * @param from       起始
     * @param to         结束
     * @param limit      点数上限
     * @return 趋势
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Operation(summary = "我的指标趋势")
    @MiyfPermission(code = "health:user:trend:view")
    @GetMapping("/me/trends")
    public ApiResult<HealthTrendVo> myTrend(@RequestParam String metricCode,
                                            @RequestParam(required = false) Instant from,
                                            @RequestParam(required = false) Instant to,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(healthCrudApplicationService.myTrend(metricCode, from, to, limit));
    }
}
