package cn.miyf.kitchen.controller.admin;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.security.KitchenAdminPopedom;
import cn.miyf.kitchen.service.DishApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端菜品接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Tag(name = "管理端菜品")
@KitchenAdminPopedom
@RestController
@RequestMapping("/admin/dishes")
@RequiredArgsConstructor
public class AdminDishController {

    private final DishApplicationService dishApplicationService;

    
    /**
     * 菜品分页。
     *
     * @param qo 查询条件
     * @return 分页结果
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "菜品分页")
    @MiyfPermission(code = "kitchen:dish:list")
    @GetMapping
    public ApiResult<PageResult<DishVo>> page(DishPageQo qo) {
        return ApiResult.ok(dishApplicationService.pageAdmin(qo));
    }

    /**
     * 菜品详情。
     *
     * @param id 菜品 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "菜品详情")
    @MiyfPermission(code = "kitchen:dish:list")
    @GetMapping("/{id}")
    public ApiResult<DishVo> detail(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.getAdminDetail(id));
    }

    /**
     * 创建菜品（草稿）。
     *
     * @param dto 请求
     * @return 新建菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "创建菜品")
    @MiyfPermission(code = "kitchen:dish:create")
    @PostMapping
    public ApiResult<DishVo> create(@Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.create(dto));
    }

    /**
     * 更新菜品。
     *
     * @param id  菜品 ID
     * @param dto 请求
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "更新菜品")
    @MiyfPermission(code = "kitchen:dish:update")
    @PutMapping("/{id}")
    public ApiResult<DishVo> update(@PathVariable Long id, @Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.update(id, dto));
    }

    /**
     * 删除菜品（逻辑删除）。
     *
     * @param id 菜品 ID
     * @return 空
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "删除菜品")
    @MiyfPermission(code = "kitchen:dish:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        dishApplicationService.delete(id);
        return ApiResult.ok();
    }

    /**
     * 上架菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "上架菜品")
    @MiyfPermission(code = "kitchen:dish:publish")
    @PostMapping("/{id}/publish")
    public ApiResult<DishVo> publish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.publish(id));
    }

    /**
     * 下架菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "下架菜品")
    @MiyfPermission(code = "kitchen:dish:unpublish")
    @PostMapping("/{id}/unpublish")
    public ApiResult<DishVo> unpublish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.unpublish(id));
    }
}
