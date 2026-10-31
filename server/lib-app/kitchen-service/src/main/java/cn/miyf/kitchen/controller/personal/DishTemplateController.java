package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.DishTemplateSaveDto;
import cn.miyf.kitchen.bean.qo.DishTemplatePageQo;
import cn.miyf.kitchen.bean.vo.DishTemplateVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.DishTemplateApplicationService;
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

/**
 * 菜品库（菜品模板 / 候选菜）成员端接口。
 * <p>
 * 厨房内所有已绑定成员均可创建与维护菜品模板；模板独立于点菜菜单，
 * 以 {@code /dishes/templates} 与现有 {@code /dishes}（点菜菜品）区分，避免语义混淆。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
@Tag(name = "菜品库")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/dishes/templates")
@RequiredArgsConstructor
public class DishTemplateController {

    private final DishTemplateApplicationService dishTemplateApplicationService;

    /**
     * 菜品库分页：支持状态、我创建的、最近使用与关键字筛选。
     *
     * @param qo 查询条件
     * @return 分页结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "菜品库列表")
    @MiyfPermission(code = "kitchen:user:dish-template:list")
    @GetMapping
    public ApiResult<PageResult<DishTemplateVo>> page(DishTemplatePageQo qo) {
        return ApiResult.ok(dishTemplateApplicationService.page(qo));
    }

    /**
     * 菜品模板详情。
     *
     * @param id 模板 ID
     * @return 模板详情
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "菜品模板详情")
    @MiyfPermission(code = "kitchen:user:dish-template:detail")
    @GetMapping("/{id}")
    public ApiResult<DishTemplateVo> detail(@PathVariable Long id) {
        return ApiResult.ok(dishTemplateApplicationService.detail(id));
    }

    /**
     * 上传菜品模板图片（封面、步骤图）。
     *
     * @param file 图片文件
     * @return 上传结果（含 /r/{id}）
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "上传菜品模板图片")
    @MiyfPermission(code = "kitchen:user:dish-template:upload")
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UploadedFileVo> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(dishTemplateApplicationService.uploadImage(file));
    }

    /**
     * 创建菜品模板（草稿或预创建）。
     *
     * @param dto 保存请求
     * @return 新建模板 ID
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "创建菜品模板")
    @MiyfPermission(code = "kitchen:user:dish-template:create")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody DishTemplateSaveDto dto) {
        return ApiResult.ok(dishTemplateApplicationService.create(dto));
    }

    /**
     * 更新菜品模板。
     *
     * @param id  模板 ID
     * @param dto 保存请求
     * @return 空结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "更新菜品模板")
    @MiyfPermission(code = "kitchen:user:dish-template:update")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody DishTemplateSaveDto dto) {
        dishTemplateApplicationService.update(id, dto);
        return ApiResult.ok();
    }

    /**
     * 删除菜品模板（仅草稿/预创建且无做菜记录）。
     *
     * @param id 模板 ID
     * @return 空结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "删除菜品模板")
    @MiyfPermission(code = "kitchen:user:dish-template:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        dishTemplateApplicationService.delete(id);
        return ApiResult.ok();
    }

    /**
     * 发布菜品模板，发布后可被提交做菜任务。
     *
     * @param id 模板 ID
     * @return 空结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "发布菜品模板")
    @MiyfPermission(code = "kitchen:user:dish-template:publish")
    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        dishTemplateApplicationService.publish(id);
        return ApiResult.ok();
    }

    /**
     * 归档菜品模板；已创建的做菜任务不受影响。
     *
     * @param id 模板 ID
     * @return 空结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Operation(summary = "归档菜品模板")
    @MiyfPermission(code = "kitchen:user:dish-template:archive")
    @PostMapping("/{id}/archive")
    public ApiResult<Void> archive(@PathVariable Long id) {
        dishTemplateApplicationService.archive(id);
        return ApiResult.ok();
    }
}
