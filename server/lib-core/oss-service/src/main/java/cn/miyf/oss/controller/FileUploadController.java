package cn.miyf.oss.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.oss.bean.dto.FileUploadCommand;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.service.FileResourceApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 平台通用文件上传；需 {@code file:upload} 权限。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Tag(name = "文件上传")
@SystemSettingsPopedom
@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileResourceApplicationService fileResourceApplicationService;

    /**
     * 上传图片文件（JPG / PNG / WebP）。
     *
     * @param file             表单字段名 file
     * @param appCode          产品应用编码（kitchen / health 等）
     * @param source           来源（可选）
     * @param temp             是否临时文件
     * @param compress         是否压缩
     * @param accessPermission 访问权限
     * @return 上传结果（含文件 ID 与 /r/{id}）
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "上传图片")
    @MiyfPermission(code = "file:upload")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UploadedFileVo> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("appCode") String appCode,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "temp", required = false, defaultValue = "false") boolean temp,
            @RequestParam(value = "compress", required = false, defaultValue = "false") boolean compress,
            @RequestParam(value = "accessPermission", required = false) FileAccessPermission accessPermission) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "请选择文件");
        }
        FileUploadCommand command = new FileUploadCommand()
                .setAppCode(appCode)
                .setSource(source)
                .setTemp(temp)
                .setCompress(compress)
                .setAccessPermission(accessPermission == null ? FileAccessPermission.OWNER : accessPermission);
        try (InputStream in = file.getInputStream()) {
            UploadedFileVo vo = fileResourceApplicationService.store(
                    in,
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    command
            );
            return ApiResult.ok(vo);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取上传流失败");
        }
    }
}
