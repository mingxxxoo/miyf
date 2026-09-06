package cn.miyf.monitor.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.monitor.MonitorApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 监控概览 API。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-监控")
@PopedomGroup(value = "10040000", name = "系统设置", product = "system", sort = 8)
@RestController
@RequestMapping("/api/admin/system/monitor")
public class AdminMonitorController {

    private final MonitorApplicationService monitorApplicationService;

    public AdminMonitorController(MonitorApplicationService monitorApplicationService) {
        this.monitorApplicationService = monitorApplicationService;
    }

    @Operation(summary = "JVM / Redis / 数据库组件状态")
    @MiyfPermission(code = "sys:monitor:view", name = "监控概览", groupCode = "sys_monitor", groupName = "监控")
    @RequirePermission({"sys:monitor:view"})
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(monitorApplicationService.overview());
    }
}
