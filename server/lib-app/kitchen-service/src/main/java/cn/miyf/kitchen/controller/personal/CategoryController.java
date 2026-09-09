package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.CategoryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端分类接口（只读启用分类）。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Tag(name = "用户端分类")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryApplicationService categoryApplicationService;

    /**
     * 启用中的分类列表。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "启用分类列表")
    @MiyfPermission(code = "kitchen:user:category:list")
    @GetMapping
    public ApiResult<List<CategoryVo>> list() {
        return ApiResult.ok(categoryApplicationService.listEnabled());
    }
}
