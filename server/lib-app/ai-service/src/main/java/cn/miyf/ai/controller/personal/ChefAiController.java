package cn.miyf.ai.controller.personal;

import cn.miyf.ai.bean.dto.ChefDishAiExtractDto;
import cn.miyf.ai.bean.vo.ChefDishAiExtractVo;
import cn.miyf.ai.chef.ChefDishAiService;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 厨师端 AI 建菜接口。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Tag(name = "厨师 AI")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/chef/ai")
@RequiredArgsConstructor
public class ChefAiController {

    private final ChefDishAiService chefDishAiService;

    /**
     * 文本或网页链接抽取菜品草稿（不落库）。
     *
     * @param dto 请求
     * @return 草稿确认页数据
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Operation(summary = "AI 建菜抽取")
    @MiyfPermission(code = "kitchen:user:dish:list")
    @PostMapping("/dishes/extract")
    public ApiResult<ChefDishAiExtractVo> extract(@Valid @RequestBody ChefDishAiExtractDto dto) {
        return ApiResult.ok(chefDishAiService.extract(dto));
    }
}
