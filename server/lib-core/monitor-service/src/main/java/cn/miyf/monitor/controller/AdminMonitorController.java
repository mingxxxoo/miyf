package cn.miyf.monitor.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.monitor.MonitorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统监控概览 API。
 * 汇总 JVM、Redis、数据库连接池等组件健康快照。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-监控")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system/monitor")
public class AdminMonitorController {

    private final MonitorApplicationService monitorApplicationService;

    /**
     * 构造控制器。
     *
     * @param monitorApplicationService 监控应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public AdminMonitorController(MonitorApplicationService monitorApplicationService) {
        this.monitorApplicationService = monitorApplicationService;
    }

    /**
     * 获取监控概览（主机/JVM/Redis/数据库等）。
     *
     * @return 概览 Map
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "JVM / Redis / 数据库组件状态")
    @MiyfPermission(code = "sys:monitor:view")
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(monitorApplicationService.overview());
    }
}
