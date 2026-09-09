package cn.miyf.kitchen.controller.admin;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.RequirePermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.UserPageQo;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import cn.miyf.kitchen.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端厨房用户接口。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "管理端用户")
@KitchenAdminPopedom
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserApplicationService userApplicationService;

    
    /**
     * 用户分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "厨房用户分页")
    @MiyfPermission(code = "user:list")
    @GetMapping
    public ApiResult<PageResult<UserVo>> page(UserPageQo qo) {
        return ApiResult.ok(userApplicationService.pageAdmin(qo));
    }

    /**
     * 用户详情。
     *
     * @param id 用户 ID
     * @return 详情
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "厨房用户详情")
    @MiyfPermission(code = "user:detail")
    @RequirePermission({"user:detail", "user:list"})
    @GetMapping("/{id}")
    public ApiResult<UserVo> detail(@PathVariable Long id) {
        return ApiResult.ok(userApplicationService.getAdmin(id));
    }
}
