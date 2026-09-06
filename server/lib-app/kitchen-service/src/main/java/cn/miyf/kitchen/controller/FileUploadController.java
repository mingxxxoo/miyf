package cn.miyf.kitchen.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.infrastructure.storage.FileStorageService;
import cn.miyf.infrastructure.storage.StoredFile;
import cn.miyf.kitchen.bean.vo.UploadedFileVo;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 通用文件上传（管理端菜品图等）；需 {@code kitchen:file:upload} 权限。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Tag(name = "文件上传")
@PopedomGroup(value = "11030000", name = "管理员", product = "kitchen", sort = 10)
@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * 构造控制器。
     *
     * @param fileStorageService 存储服务
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传图片文件（JPG / PNG / WebP）。
     *
     * @param file 表单字段名 file
     * @return 上传结果（含公开 URL）
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Operation(summary = "上传图片")
    @MiyfPermission(code = "kitchen:file:upload", name = "上传文件", groupCode = "kitchen_file", groupName = "文件上传")
    @RequirePermission({"kitchen:file:upload"})
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<UploadedFileVo> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "请选择文件");
        }
        try (InputStream in = file.getInputStream()) {
            StoredFile stored = fileStorageService.store(
                    in,
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename()
            );
            UploadedFileVo vo = new UploadedFileVo()
                    .setObjectKey(stored.objectKey())
                    .setUrl(stored.url())
                    .setContentType(stored.contentType())
                    .setSize(stored.size());
            return ApiResult.ok(vo);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取上传流失败");
        }
    }
}

