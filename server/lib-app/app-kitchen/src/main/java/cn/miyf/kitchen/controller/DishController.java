package cn.miyf.kitchen.controller;

import cn.miyf.kitchen.service.CommentApplicationService;
import cn.miyf.kitchen.service.DishApplicationService;
import cn.miyf.kitchen.service.RecipeApplicationService;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.CommentPageQo;
import cn.miyf.kitchen.bean.qo.DishPageQo;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.kitchen.bean.vo.RecipeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端菜品接口：列表、搜索、详情、热门、推荐、菜谱、评价。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:30
 */
@Tag(name = "用户端菜品")
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishApplicationService dishApplicationService;
    private final RecipeApplicationService recipeApplicationService;
    private final CommentApplicationService commentApplicationService;

    /**
     * 构造控制器。
     *
     * @param dishApplicationService    菜品服务
     * @param recipeApplicationService  菜谱服务
     * @param commentApplicationService 评价服务
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    public DishController(DishApplicationService dishApplicationService,
                          RecipeApplicationService recipeApplicationService,
                          CommentApplicationService commentApplicationService) {
        this.dishApplicationService = dishApplicationService;
        this.recipeApplicationService = recipeApplicationService;
        this.commentApplicationService = commentApplicationService;
    }

    /**
     * 已上架菜品分页（支持分类、关键词、推荐筛选）。
     *
     * @param qo 查询条件
     * @return 分页结果
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "上架菜品分页")
    @GetMapping
    public ApiResult<PageResult<DishVo>> page(DishPageQo qo) {
        return ApiResult.ok(dishApplicationService.pageUser(qo));
    }

    /**
     * 热门菜品。
     *
     * @param limit 条数，默认 10
     * @return 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "热门菜品")
    @GetMapping("/hot")
    public ApiResult<List<DishVo>> hot(@RequestParam(required = false) Integer limit) {
        return ApiResult.ok(dishApplicationService.listHot(limit));
    }

    /**
     * 推荐菜品。
     *
     * @param limit 条数，默认 10
     * @return 列表
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "推荐菜品")
    @GetMapping("/recommend")
    public ApiResult<List<DishVo>> recommend(@RequestParam(required = false) Integer limit) {
        return ApiResult.ok(dishApplicationService.listRecommend(limit));
    }

    /**
     * 已上架菜品的菜谱。
     *
     * @param id 菜品 ID
     * @return 菜谱
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    @Operation(summary = "菜品菜谱")
    @GetMapping("/{id}/recipe")
    public ApiResult<RecipeVo> recipe(@PathVariable Long id) {
        return ApiResult.ok(recipeApplicationService.getByDishIdUser(id));
    }

    /**
     * 菜品公开评价分页（仅 NORMAL）。
     *
     * @param id 菜品 ID
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "菜品评价列表")
    @GetMapping("/{id}/comments")
    public ApiResult<PageResult<CommentVo>> comments(@PathVariable Long id, CommentPageQo qo) {
        return ApiResult.ok(commentApplicationService.pageByDish(id, qo));
    }

    /**
     * 已上架菜品详情。
     *
     * @param id 菜品 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:30 XieMingJie Created.
     */
    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public ApiResult<DishVo> detail(@PathVariable Long id) {
        return ApiResult.ok(dishApplicationService.getUserDetail(id));
    }
}
