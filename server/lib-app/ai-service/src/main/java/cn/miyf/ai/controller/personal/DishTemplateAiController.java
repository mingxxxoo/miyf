package cn.miyf.ai.controller.personal;

import cn.miyf.ai.bean.dto.DishTemplateAiTextDto;
import cn.miyf.ai.bean.vo.DishTemplateAiExtractVo;
import cn.miyf.ai.chef.DishTemplateAiService;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 菜品库 AI 建菜接口（文本 / 链接 / 图片）。
 * <p>
 * 只返回草稿，绝不自动创建或发布模板；成员确认后走 {@code /dishes/templates} 保存。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
@Tag(name = "菜品库 AI")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/chef/ai/dish-templates")
@RequiredArgsConstructor
public class DishTemplateAiController {

    private final DishTemplateAiService dishTemplateAiService;

    /**
     * 文本或网页链接抽取菜品草稿。
     *
     * @param dto 文本与链接二选一
     * @return 确认页草稿
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    @Operation(summary = "AI 菜品草稿（文本/链接）")
    @MiyfPermission(code = "kitchen:user:dish-template:create")
    @PostMapping("/extract")
    public ApiResult<DishTemplateAiExtractVo> extract(@Valid @RequestBody DishTemplateAiTextDto dto) {
        return ApiResult.ok(dishTemplateAiService.extractText(dto));
    }

    /**
     * 菜品照片抽取菜品草稿。
     *
     * @param file 图片文件
     * @param text 可选补充说明
     * @return 确认页草稿
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    @Operation(summary = "AI 菜品草稿（图片）")
    @MiyfPermission(code = "kitchen:user:dish-template:create")
    @PostMapping(value = "/extract/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<DishTemplateAiExtractVo> extractImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "text", required = false) String text) {
        return ApiResult.ok(dishTemplateAiService.extractImage(file, text));
    }
}
