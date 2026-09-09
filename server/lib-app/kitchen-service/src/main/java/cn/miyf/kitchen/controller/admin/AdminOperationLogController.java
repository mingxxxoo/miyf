package cn.miyf.kitchen.controller.admin;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.OperationLogPageQo;
import cn.miyf.kitchen.bean.vo.OperationLogVo;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import cn.miyf.kitchen.service.OperationLogApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@KitchenAdminPopedom
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final OperationLogApplicationService operationLogApplicationService;

    
    /**
     * 操作日志分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "操作日志分页")
    @MiyfPermission(code = "operation-log:list")
    @GetMapping
    public ApiResult<PageResult<OperationLogVo>> page(OperationLogPageQo qo) {
        return ApiResult.ok(operationLogApplicationService.pageAdmin(qo));
    }
}
