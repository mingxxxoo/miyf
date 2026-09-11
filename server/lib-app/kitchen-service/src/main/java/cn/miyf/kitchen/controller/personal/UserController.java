package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.ApiResult;
import cn.miyf.kitchen.bean.dto.UserProfileSaveDto;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小程序「我的」资料：查询、改昵称头像、上传头像。
 *
 * @author XieMingJie
 * @since 2026-09-11
 */
@Tag(name = "用户端资料")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    /**
     * 当前登录用户资料。
     *
     * @return 资料
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "我的资料")
    @MiyfPermission(code = "kitchen:user:profile:view")
    @GetMapping("/me")
    public ApiResult<UserVo> me() {
        return ApiResult.ok(userApplicationService.getMe(SecurityUtils.currentUserId()));
    }

    /**
     * 更新昵称 / 头像 URL。
     *
     * @param dto 资料
     * @return 更新后资料
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "更新我的资料")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @PutMapping("/me")
    public ApiResult<UserVo> updateMe(@Valid @RequestBody UserProfileSaveDto dto) {
        return ApiResult.ok(userApplicationService.updateMe(SecurityUtils.currentUserId(), dto));
    }

    /**
     * 上传并保存头像。
     * 同时接受 POST（小程序 uploadFile）与 PUT。
     *
     * @param file 图片
     * @return 更新后资料
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "上传我的头像")
    @MiyfPermission(code = "kitchen:user:profile:update")
    @RequestMapping(value = "/me/avatar", method = {RequestMethod.POST, RequestMethod.PUT},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UserVo> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(userApplicationService.uploadAvatar(SecurityUtils.currentUserId(), file));
    }
}
