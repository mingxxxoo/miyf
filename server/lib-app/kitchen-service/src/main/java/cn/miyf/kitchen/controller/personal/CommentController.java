package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.CommentCreateDto;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.CommentApplicationService;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端评价接口。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:55
 */
@Tag(name = "用户端评价")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentApplicationService commentApplicationService;

    /**
     * 上传评价配图。
     *
     * @param file 图片文件
     * @return 上传结果（含 /r/{id}）
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "上传评价配图")
    @MiyfPermission(code = "kitchen:user:comment:create")
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UploadedFileVo> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(commentApplicationService.uploadImage(file));
    }

    /**
     * 发表评价（需完成预约且一单一菜一次）。
     *
     * @param dto 请求
     * @return 评价
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Operation(summary = "发表评价")
    @MiyfPermission(code = "kitchen:user:comment:create")
    @PostMapping
    public ApiResult<CommentVo> create(@Valid @RequestBody CommentCreateDto dto) {
        return ApiResult.ok(commentApplicationService.create(dto));
    }
}
