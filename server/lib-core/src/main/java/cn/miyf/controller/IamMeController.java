package cn.miyf.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.service.IamApplicationService;
import cn.miyf.bean.vo.SysMenuTreeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前登录用户 IAM 接口。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-当前用户")
@RestController
@RequestMapping("/api/iam/me")
public class IamMeController {

    private final IamApplicationService iamApplicationService;

    /**
     * 构造控制器。
     *
     * @param iamApplicationService IAM 服务
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public IamMeController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    /**
     * 当前用户可见菜单树（需登录；细粒度按权限码过滤）。
     *
     * @return 菜单树
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "当前用户菜单树")
    @GetMapping("/menus")
    public ApiResult<List<SysMenuTreeVo>> menus() {
        return ApiResult.ok(iamApplicationService.currentUserMenus());
    }
}
