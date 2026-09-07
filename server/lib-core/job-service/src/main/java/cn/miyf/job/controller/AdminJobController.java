package cn.miyf.job.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.job.JobDescriptor;
import cn.miyf.job.JobRegistry;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.SystemSettingsPopedom;
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

    public AdminJobController(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    @Operation(summary = "已注册定时任务列表")
    @MiyfPermission(code = "sys:job:list")
    @GetMapping
    public ApiResult<List<JobDescriptor>> list() {
        return ApiResult.ok(jobRegistry.list());
    }

    @Operation(summary = "手动触发任务（禁用状态也可强制执行一次）")
    @MiyfPermission(code = "sys:job:trigger")
    @PostMapping("/{code:.+}/trigger")
    public ApiResult<JobDescriptor> trigger(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.trigger(code));
    }

    @Operation(summary = "启用任务调度")
    @MiyfPermission(code = "sys:job:enable")
    @PostMapping("/{code:.+}/enable")
    public ApiResult<JobDescriptor> enable(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.setEnabled(code, true));
    }

    @Operation(summary = "停用任务调度")
    @MiyfPermission(code = "sys:job:disable")
    @PostMapping("/{code:.+}/disable")
    public ApiResult<JobDescriptor> disable(@PathVariable String code) {
        return ApiResult.ok(jobRegistry.setEnabled(code, false));
    }
}
