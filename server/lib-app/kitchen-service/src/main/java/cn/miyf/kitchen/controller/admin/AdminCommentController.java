package cn.miyf.kitchen.controller.admin;

import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.qo.CommentPageQo;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.bean.vo.DishRatingVo;
import cn.miyf.kitchen.service.CommentApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 管理端评价接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Tag(name = "管理端评价")
@PopedomGroup(value = "11030000", name = "管理员", product = "kitchen", sort = 10)
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentApplicationService commentApplicationService;

    /**
     * 构造控制器。
     *
     * @param commentApplicationService 评价服务
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public AdminCommentController(CommentApplicationService commentApplicationService) {
        this.commentApplicationService = commentApplicationService;
    }

    /**
     * 评价分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "评价分页")
    @MiyfPermission(code = "kitchen:comment:list", name = "评价列表", groupCode = "kitchen_comment", groupName = "评价管理")
    @RequirePermission({"kitchen:comment:list"})
    @GetMapping
    public ApiResult<PageResult<CommentVo>> page(CommentPageQo qo) {
        return ApiResult.ok(commentApplicationService.pageAdmin(qo));
    }

    /**
     * 隐藏评价。
     *
     * @param id 评价 ID
     * @return 评价
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "隐藏评价")
    @MiyfPermission(code = "kitchen:comment:hide", name = "隐藏评价", groupCode = "kitchen_comment", groupName = "评价管理")
    @RequirePermission({"kitchen:comment:hide"})
    @PostMapping("/{id}/hide")
    public ApiResult<CommentVo> hide(@PathVariable Long id) {
        return ApiResult.ok(commentApplicationService.hide(id));
    }

    /**
     * 恢复评价。
     *
     * @param id 评价 ID
     * @return 评价
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "恢复评价")
    @MiyfPermission(code = "kitchen:comment:restore", name = "恢复评价", groupCode = "kitchen_comment", groupName = "评价管理")
    @RequirePermission({"kitchen:comment:restore"})
    @PostMapping("/{id}/restore")
    public ApiResult<CommentVo> restore(@PathVariable Long id) {
        return ApiResult.ok(commentApplicationService.restore(id));
    }

    /**
     * 删除评价（逻辑删除）。
     *
     * @param id 评价 ID
     * @return 空
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "删除评价")
    @MiyfPermission(code = "kitchen:comment:delete", name = "删除评价", groupCode = "kitchen_comment", groupName = "评价管理")
    @RequirePermission({"kitchen:comment:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        commentApplicationService.delete(id);
        return ApiResult.ok();
    }

    /**
     * 按菜品重算评分（维护/纠偏）。
     *
     * @param dishId 菜品 ID
     * @return 评分结果
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "重算菜品评分")
    @MiyfPermission(code = "kitchen:comment:list", name = "评价列表", groupCode = "kitchen_comment", groupName = "评价管理")
    @RequirePermission({"kitchen:comment:list", "kitchen:dish:update"})
    @PostMapping("/rebuild-rating/{dishId}")
    public ApiResult<DishRatingVo> rebuildRating(@PathVariable Long dishId) {
        return ApiResult.ok(commentApplicationService.rebuildDishRating(dishId));
    }
}
