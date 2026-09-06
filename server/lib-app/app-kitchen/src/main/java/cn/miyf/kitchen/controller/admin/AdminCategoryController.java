package cn.miyf.kitchen.controller.admin;

import cn.miyf.kitchen.service.CategoryApplicationService;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.CategorySaveDto;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.RequirePermission;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端分类接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Tag(name = "管理端分类")
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryApplicationService categoryApplicationService;

    /**
     * 构造控制器。
     *
     * @param categoryApplicationService 分类服务
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public AdminCategoryController(CategoryApplicationService categoryApplicationService) {
        this.categoryApplicationService = categoryApplicationService;
    }

    /**
     * 全部分类列表。
     *
     * @return 分类列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "分类列表")
    @MiyfPermission(code = "kitchen:category:list", name = "分类列表", groupCode = "kitchen_category", groupName = "分类管理")
    @RequirePermission({"kitchen:category:list"})
    @GetMapping
    public ApiResult<List<CategoryVo>> list() {
        return ApiResult.ok(categoryApplicationService.listAll());
    }

    /**
     * 创建分类。
     *
     * @param dto 请求
     * @return 新建分类
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "创建分类")
    @MiyfPermission(code = "kitchen:category:create", name = "创建分类", groupCode = "kitchen_category", groupName = "分类管理")
    @RequirePermission({"kitchen:category:create"})
    @PostMapping
    public ApiResult<CategoryVo> create(@Valid @RequestBody CategorySaveDto dto) {
        return ApiResult.ok(categoryApplicationService.create(dto));
    }

    /**
     * 更新分类（含排序、启停）。
     *
     * @param id  分类 ID
     * @param dto 请求
     * @return 更新后分类
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "更新分类")
    @MiyfPermission(code = "kitchen:category:update", name = "更新分类", groupCode = "kitchen_category", groupName = "分类管理")
    @RequirePermission({"kitchen:category:update"})
    @PutMapping("/{id}")
    public ApiResult<CategoryVo> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDto dto) {
        return ApiResult.ok(categoryApplicationService.update(id, dto));
    }

    /**
     * 删除分类。
     *
     * @param id 分类 ID
     * @return 空
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "删除分类")
    @MiyfPermission(code = "kitchen:category:delete", name = "删除分类", groupCode = "kitchen_category", groupName = "分类管理")
    @RequirePermission({"kitchen:category:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        categoryApplicationService.delete(id);
        return ApiResult.ok();
    }
}
