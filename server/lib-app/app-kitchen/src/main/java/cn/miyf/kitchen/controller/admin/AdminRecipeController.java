package cn.miyf.kitchen.controller.admin;

import cn.miyf.kitchen.service.RecipeApplicationService;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.RequirePermission;
import cn.miyf.kitchen.bean.vo.RecipeVo;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端菜谱接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Tag(name = "管理端菜谱")
@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    private final RecipeApplicationService recipeApplicationService;

    /**
     * 构造控制器。
     *
     * @param recipeApplicationService 菜谱服务
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    public AdminRecipeController(RecipeApplicationService recipeApplicationService) {
        this.recipeApplicationService = recipeApplicationService;
    }

    /**
     * 菜谱列表；可按菜品 ID 过滤。
     *
     * @param dishId 菜品 ID，可空
     * @return 列表或单条包装为列表
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "菜谱列表")
    @MiyfPermission(code = "kitchen:recipe:list", name = "菜谱列表", groupCode = "kitchen_recipe", groupName = "菜谱管理")
    @RequirePermission({"kitchen:recipe:list"})
    @GetMapping
    public ApiResult<List<RecipeVo>> list(@RequestParam(required = false) Long dishId) {
        if (dishId != null) {
            return ApiResult.ok(recipeApplicationService.listByDishId(dishId));
        }
        return ApiResult.ok(recipeApplicationService.listAll());
    }

    /**
     * 菜谱详情。
     *
     * @param id 菜谱 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "菜谱详情")
    @MiyfPermission(code = "kitchen:recipe:list", name = "菜谱列表", groupCode = "kitchen_recipe", groupName = "菜谱管理")
    @RequirePermission({"kitchen:recipe:list"})
    @GetMapping("/{id}")
    public ApiResult<RecipeVo> detail(@PathVariable Long id) {
        return ApiResult.ok(recipeApplicationService.getById(id));
    }

    /**
     * 创建菜谱。
     *
     * @param dto 请求
     * @return 新建菜谱
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "创建菜谱")
    @MiyfPermission(code = "kitchen:recipe:create", name = "创建菜谱", groupCode = "kitchen_recipe", groupName = "菜谱管理")
    @RequirePermission({"kitchen:recipe:create"})
    @PostMapping
    public ApiResult<RecipeVo> create(@Valid @RequestBody RecipeSaveDto dto) {
        return ApiResult.ok(recipeApplicationService.create(dto));
    }

    /**
     * 更新菜谱。
     *
     * @param id  菜谱 ID
     * @param dto 请求
     * @return 更新后菜谱
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "更新菜谱")
    @MiyfPermission(code = "kitchen:recipe:update", name = "更新菜谱", groupCode = "kitchen_recipe", groupName = "菜谱管理")
    @RequirePermission({"kitchen:recipe:update"})
    @PutMapping("/{id}")
    public ApiResult<RecipeVo> update(@PathVariable Long id, @Valid @RequestBody RecipeSaveDto dto) {
        return ApiResult.ok(recipeApplicationService.update(id, dto));
    }

    /**
     * 删除菜谱。
     *
     * @param id 菜谱 ID
     * @return 空
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "删除菜谱")
    @MiyfPermission(code = "kitchen:recipe:delete", name = "删除菜谱", groupCode = "kitchen_recipe", groupName = "菜谱管理")
    @RequirePermission({"kitchen:recipe:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        recipeApplicationService.delete(id);
        return ApiResult.ok();
    }
}
