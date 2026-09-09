package cn.miyf.oss.controller;

import cn.miyf.oss.bean.entity.SysResourceIndexEntity;
import cn.miyf.oss.service.FileResourceApplicationService;
import cn.miyf.web.RootMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;

/**
 * 文件读取：{@code GET /r/{fileId}}。
 * 放行条件：Redis 临时授权 / 上传人 / 公共文件（管理员始终可访问）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Tag(name = "文件资源")
@RootMapping
@RestController
@RequiredArgsConstructor
public class FileResourceController {

    private final FileResourceApplicationService fileResourceApplicationService;

    /**
     * 按文件 ID 读取内容。
     *
     * @param fileId 文件 ID
     * @return 文件流
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Operation(summary = "按文件 ID 读取")
    @GetMapping("/r/{fileId}")
    public ResponseEntity<InputStreamResource> read(@PathVariable("fileId") Long fileId) {
        fileResourceApplicationService.assertReadable(fileId);
        SysResourceIndexEntity meta = fileResourceApplicationService.requireMeta(fileId);
        InputStream stream = fileResourceApplicationService.open(fileId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (StringUtils.hasText(meta.getMimeType())) {
            mediaType = MediaType.parseMediaType(meta.getMimeType());
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        if (meta.getFileSize() != null && meta.getFileSize() >= 0) {
            builder.contentLength(meta.getFileSize());
        }
        return builder.body(new InputStreamResource(stream));
    }
}
