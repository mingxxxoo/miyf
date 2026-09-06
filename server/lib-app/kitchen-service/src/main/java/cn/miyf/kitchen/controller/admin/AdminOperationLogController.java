package cn.miyf.kitchen.controller.admin;

import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.OperationLogPageQo;
import cn.miyf.kitchen.bean.vo.OperationLogVo;
import cn.miyf.kitchen.service.OperationLogApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端操作日志接口。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "管理端操作日志")
@PopedomGroup(value = "11030000", name = "管理员", product = "kitchen", sort = 10)
@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    private final OperationLogApplicationService operationLogApplicationService;

    /**
     * 构造控制器。
     *
     * @param operationLogApplicationService 日志服务
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public AdminOperationLogController(OperationLogApplicationService operationLogApplicationService) {
        this.operationLogApplicationService = operationLogApplicationService;
    }

    /**
     * 操作日志分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "操作日志分页")
    @MiyfPermission(code = "operation-log:list", name = "操作日志列表", groupCode = "kitchen_oplog", groupName = "操作日志")
    @RequirePermission({"operation-log:list"})
    @GetMapping
    public ApiResult<PageResult<OperationLogVo>> page(OperationLogPageQo qo) {
        return ApiResult.ok(operationLogApplicationService.pageAdmin(qo));
    }
}
