package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.CategorySaveDto;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.dto.OrderStatusUpdateDto;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.bean.vo.RecipeVo;
import cn.miyf.kitchen.bean.vo.WxSubscribeConfigVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.CategoryApplicationService;
import cn.miyf.kitchen.service.DishApplicationService;
import cn.miyf.kitchen.service.KitchenAccessService;
import cn.miyf.kitchen.service.OrderApplicationService;
import cn.miyf.kitchen.service.OrderChefWxNotifyService;
import cn.miyf.kitchen.service.RecipeApplicationService;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 厨师菜品、分类、菜谱与接单。
 * 菜品须审核通过后方可上架；预约状态由厨师侧流转；分类与菜谱由厨师自管。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Tag(name = "厨师工作台")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/chef")
@RequiredArgsConstructor
public class ChefController {

    private final DishApplicationService dishApplicationService;
    private final OrderApplicationService orderApplicationService;
    private final CategoryApplicationService categoryApplicationService;
    private final RecipeApplicationService recipeApplicationService;
    private final OrderChefWxNotifyService orderChefWxNotifyService;
    private final KitchenAccessService kitchenAccessService;

    /**
     * 微信订阅消息配置（厨师端授权用）。
     *
     * @return 开关与模板 ID
     */
    @Operation(summary = "微信订阅消息配置")
    @MiyfPermission(code = "kitchen:user:order:list")
    @GetMapping("/wx-subscribe-config")
    public ApiResult<WxSubscribeConfigVo> wxSubscribeConfig() {
        kitchenAccessService.requireOwnedKitchen();
        String templateId = orderChefWxNotifyService.resolveTemplateId();
        boolean enabled = orderChefWxNotifyService.isEnabled() && templateId != null && !templateId.isBlank();
        return ApiResult.ok(new WxSubscribeConfigVo()
                .setEnabled(enabled)
                .setTemplateIds(enabled ? List.of(templateId.trim()) : List.of()));
    }

    /**
     * 本厨房分类列表（含停用）。
     *
     * @return 分类列表
     */
    @Operation(summary = "厨师分类列表")
    @MiyfPermission(code = "kitchen:user:category:list")
    @GetMapping("/categories")
    public ApiResult<List<CategoryVo>> categories() {
        return ApiResult.ok(categoryApplicationService.listChef());
    }

    /**
     * 创建本厨房分类。
     *
     * @param dto 分类内容
     * @return 新建分类
     */
    @Operation(summary = "创建分类")
    @MiyfPermission(code = "kitchen:user:category:list")
    @PostMapping("/categories")
    public ApiResult<CategoryVo> createCategory(@Valid @RequestBody CategorySaveDto dto) {
        return ApiResult.ok(categoryApplicationService.createChef(dto));
    }

    /**
     * 更新本厨房分类。
     *
     * @param id  分类 ID
     * @param dto 分类内容
     * @return 更新后分类
     */
    @Operation(summary = "更新分类")
    @MiyfPermission(code = "kitchen:user:category:list")
    @PutMapping("/categories/{id}")
    public ApiResult<CategoryVo> updateCategory(@PathVariable Long id, @Valid @RequestBody CategorySaveDto dto) {
        return ApiResult.ok(categoryApplicationService.updateChef(id, dto));
    }

    /**
     * 删除本厨房分类。
     *
     * @param id 分类 ID
     * @return 空
     */
    @Operation(summary = "删除分类")
    @MiyfPermission(code = "kitchen:user:category:list")
    @DeleteMapping("/categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        categoryApplicationService.deleteChef(id);
        return ApiResult.ok();
    }

    /**
     * 本厨房菜品分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨师菜品分页")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @GetMapping("/dishes")
    public ApiResult<PageResult<DishVo>> dishes(DishPageQo qo) {
        return ApiResult.ok(dishApplicationService.pageChef(qo));
    }

    /**
     * 上传菜品封面/图集图片。
     *
     * @param file 图片文件
     * @return 上传结果（含 /r/{id}）
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "上传菜品图片")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping(value = "/dishes/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UploadedFileVo> uploadDishImage(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(dishApplicationService.uploadChefImage(file));
    }

    /**
     * 本厨房菜品详情。
     *
     * @param id 菜品 ID
     * @return 菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨师菜品详情")
    @MiyfPermission(code = "kitchen:user:dish:detail")
    @GetMapping("/dishes/{id}")
    public ApiResult<DishVo> dish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.getChefDetail(id));
    }

    /**
     * 创建菜品草稿。
     *
     * @param dto 菜品内容
     * @return 新建菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "创建菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes")
    public ApiResult<DishVo> createDish(@Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.createChef(dto));
    }

    /**
     * 更新菜品；审核中不可改，关键字段变更会回退审核状态。
     *
     * @param id  菜品 ID
     * @param dto 菜品内容
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "更新菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PutMapping("/dishes/{id}")
    public ApiResult<DishVo> updateDish(@PathVariable Long id, @Valid @RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishApplicationService.updateChef(id, dto));
    }

    /**
     * 提交菜品审核。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "提交菜品审核")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/submit-audit")
    public ApiResult<DishVo> submit(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.submitChefAudit(id));
    }

    /**
     * 撤回审核中的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "撤回菜品审核")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/withdraw-audit")
    public ApiResult<DishVo> withdraw(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.withdrawChefAudit(id));
    }

    /**
     * 上架已审核通过的菜品。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "上架菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/publish")
    public ApiResult<DishVo> publish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.publishChef(id));
    }

    /**
     * 下架本厨房上架菜品（不改审核状态，再次上架无需重审）。
     *
     * @param id 菜品 ID
     * @return 更新后菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "下架菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/{id}/unpublish")
    public ApiResult<DishVo> unpublish(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.unpublishChef(id));
    }

    /**
     * 删除本厨房草稿或已下架菜品。
     *
     * @param id 菜品 ID
     * @return 空
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "删除菜品")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @DeleteMapping("/dishes/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        dishApplicationService.deleteChef(id);
        return ApiResult.ok();
    }

    /**
     * 查询本厨菜品菜谱（可为空）。
     *
     * @param dishId 菜品 ID
     * @return 菜谱或 null
     */
    @Operation(summary = "厨师菜谱查询")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @GetMapping("/dishes/{dishId}/recipe")
    public ApiResult<RecipeVo> recipe(@PathVariable Long dishId) {
        return ApiResult.ok(recipeApplicationService.getByDishIdChef(dishId));
    }

    /**
     * 保存本厨菜品菜谱（无则创建，有则更新）。
     *
     * @param dishId 菜品 ID
     * @param dto    菜谱内容
     * @return 保存后菜谱
     */
    @Operation(summary = "保存菜谱")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PutMapping("/dishes/{dishId}/recipe")
    public ApiResult<RecipeVo> saveRecipe(@PathVariable Long dishId, @Valid @RequestBody RecipeSaveDto dto) {
        return ApiResult.ok(recipeApplicationService.saveChef(dishId, dto));
    }

    /**
     * 删除本厨菜品菜谱。
     *
     * @param dishId 菜品 ID
     * @return 空
     */
    @Operation(summary = "删除菜谱")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @DeleteMapping("/dishes/{dishId}/recipe")
    public ApiResult<Void> deleteRecipe(@PathVariable Long dishId) {
        recipeApplicationService.deleteChefByDishId(dishId);
        return ApiResult.ok();
    }

    /**
     * 本厨房预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨房预约分页")
    @MiyfPermission(code = "kitchen:user:order:list")
    @GetMapping("/orders")
    public ApiResult<PageResult<OrderVo>> orders(OrderPageQo qo) {
        return ApiResult.ok(orderApplicationService.pageChef(qo));
    }

    /**
     * 本厨房预约详情。
     *
     * @param id 预约 ID
     * @return 预约
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "厨房预约详情")
    @MiyfPermission(code = "kitchen:user:order:detail")
    @GetMapping("/orders/{id}")
    public ApiResult<OrderVo> order(@PathVariable Long id) {
        return ApiResult.ok(orderApplicationService.getChef(id));
    }

    /**
     * 厨师处理预约状态。
     *
     * @param id  预约 ID
     * @param dto 目标状态
     * @return 更新后预约
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Operation(summary = "处理预约状态")
    @MiyfPermission(code = "kitchen:user:order:list")
    @PutMapping("/orders/{id}/status")
    public ApiResult<OrderVo> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateDto dto) {
        return ApiResult.ok(orderApplicationService.updateStatusChef(id, dto));
    }
}
