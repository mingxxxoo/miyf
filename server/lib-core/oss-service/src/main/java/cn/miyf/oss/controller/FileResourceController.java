package cn.miyf.oss.controller;

import cn.miyf.oss.bean.entity.SysResourceIndexEntity;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.security.FileAccessSigner;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

/**
 * 文件读取：{@code GET /r/{fileId}}。
 * Spring Security 对该路径 permitAll（img 无法携带 Bearer）；鉴权在应用层完成。
 * 放行条件：有效签名 URL（exp/sig）/ Redis 临时授权 / 上传人 / 公共文件（管理员始终可访问）。
 * 支持查询参数 exp、sig 的 HMAC 短期签名放行，并按访问方式设置 Cache-Control。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@Tag(name = "文件资源")
@RootMapping
@RestController
@RequiredArgsConstructor
public class FileResourceController {

    private final FileResourceApplicationService fileResourceApplicationService;
    private final FileAccessSigner fileAccessSigner;

    /**
     * 按文件 ID 读取内容。
     * 优先校验 URL 上的 exp/sig；签名通过则放行，否则走主体权限与 Redis 临时授权。
     * 签名访问使用 private 短缓存，避免将带签响应当作长期公开缓存。
     *
     * @param fileId 文件 ID
     * @param exp    签名过期 unix 秒（可选）
     * @param sig    HMAC 签名（可选）
     * @return 文件流
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Operation(summary = "按文件 ID 读取")
    @GetMapping("/r/{fileId}")
    public ResponseEntity<InputStreamResource> read(
            @PathVariable("fileId") Long fileId,
            @RequestParam(value = "exp", required = false) Long exp,
            @RequestParam(value = "sig", required = false) String sig) {
        boolean signed = fileAccessSigner.verify(fileId, exp, sig);
        fileResourceApplicationService.assertReadable(fileId, signed);
        SysResourceIndexEntity meta = fileResourceApplicationService.requireMeta(fileId);
        InputStream stream = fileResourceApplicationService.open(fileId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (StringUtils.hasText(meta.getMimeType())) {
            mediaType = MediaType.parseMediaType(meta.getMimeType());
        }
        CacheControl cacheControl = resolveCacheControl(meta, signed, exp);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(cacheControl)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        if (meta.getFileSize() != null && meta.getFileSize() >= 0) {
            builder.contentLength(meta.getFileSize());
        }
        return builder.body(new InputStreamResource(stream));
    }

    /**
     * 按访问方式选择缓存策略：签名访问 private 且不超过剩余 TTL；PUBLIC 可长期 public 缓存。
     *
     * @param meta   资源元数据
     * @param signed 是否签名放行
     * @param exp    签名过期时间
     * @return Cache-Control
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    private static CacheControl resolveCacheControl(SysResourceIndexEntity meta, boolean signed, Long exp) {
        if (signed) {
            long remaining = exp == null ? 3600L : Math.max(0L, exp - Instant.now().getEpochSecond());
            long maxAge = Math.min(remaining, 3600L);
            if (maxAge <= 0) {
                return CacheControl.noStore();
            }
            return CacheControl.maxAge(Duration.ofSeconds(maxAge)).cachePrivate();
        }
        FileAccessPermission permission = meta.getAccessPermission();
        if (permission == FileAccessPermission.PUBLIC) {
            return CacheControl.maxAge(Duration.ofDays(30)).cachePublic();
        }
        return CacheControl.maxAge(Duration.ofHours(1)).cachePrivate();
    }
}
