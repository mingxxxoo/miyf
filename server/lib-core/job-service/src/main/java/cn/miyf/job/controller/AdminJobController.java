package cn.miyf.job.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.job.JobDescriptor;
import cn.miyf.job.JobRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定时任务管理 API。
 * 支持列表、手动触发、启用与停用；停用后调度跳过但可强制触发一次。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-任务")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system/jobs")
public class AdminJobController {

    private final JobRegistry jobRegistry;

    /**
     * 构造控制器。
     *
     * @param jobRegistry 任务注册表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public AdminJobController(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    /**
     * 列出已注册的定时任务及启停、最近运行状态。
     *
     * @return 任务描述列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "已注册定时任务列表")
    @MiyfPermission(code = "sys:job:list")
    @GetMapping
    public ApiResult<List<JobDescriptor>> list() {
        return ApiResult.ok(jobRegistry.list());
    }

    /**
     * 手动触发任务。禁用状态下也可强制执行一次。
     *
     * @param code 任务编码（支持含点路径）
     * @return 触发后的任务描述
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "手动触发任务（禁用状态也可强制执行一次）")
    @MiyfPermission(code = "sys:job:trigger")
    @PostMapping("/{code:.+}/trigger")
    public ApiResult<JobDescriptor> trigger(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.trigger(code));
    }

    /**
     * 启用任务调度。
     *
     * @param code 任务编码
     * @return 更新后的任务描述
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "启用任务调度")
    @MiyfPermission(code = "sys:job:enable")
    @PostMapping("/{code:.+}/enable")
    public ApiResult<JobDescriptor> enable(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.setEnabled(code, true));
    }

    /**
     * 停用任务调度；调度入口将跳过执行。
     *
     * @param code 任务编码
     * @return 更新后的任务描述
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "停用任务调度")
    @MiyfPermission(code = "sys:job:disable")
    @PostMapping("/{code:.+}/disable")
    public ApiResult<JobDescriptor> disable(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.setEnabled(code, false));
    }
}
