package cn.miyf.kitchen.controller.admin;

import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.vo.DashboardStatsVo;
import cn.miyf.kitchen.service.DashboardApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端仪表盘接口。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "管理端仪表盘")
@KitchenAdminPopedom
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    /**
     * 构造控制器。
     *
     * @param dashboardApplicationService 仪表盘服务
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public AdminDashboardController(DashboardApplicationService dashboardApplicationService) {
        this.dashboardApplicationService = dashboardApplicationService;
    }

    /**
     * 仪表盘统计。
     *
     * @return 统计
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "仪表盘统计")
    @MiyfPermission(code = "iam:dashboard:view")
    @GetMapping("/stats")
    public ApiResult<DashboardStatsVo> stats() {
        return ApiResult.ok(dashboardApplicationService.stats());
    }
}
