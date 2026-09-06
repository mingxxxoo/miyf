package cn.miyf.kitchen.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.CommentCreateDto;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.service.CommentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端评价接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Tag(name = "用户端评价")
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentApplicationService commentApplicationService;

    /**
     * 构造控制器。
     *
     * @param commentApplicationService 评价服务
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    public CommentController(CommentApplicationService commentApplicationService) {
        this.commentApplicationService = commentApplicationService;
    }

    /**
     * 发表评价（需完成预约且一单一菜一次）。
     *
     * @param dto 请求
     * @return 评价
     * @history 1.00 2026-09-04 17:55 XieMingJie Created.
     */
    @Operation(summary = "发表评价")
    @PostMapping
    public ApiResult<CommentVo> create(@Valid @RequestBody CommentCreateDto dto) {
        return ApiResult.ok(commentApplicationService.create(dto));
    }
}
