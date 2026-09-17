package cn.miyf.oss.service;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.config.FileStorageProperties;
import cn.miyf.infrastructure.storage.FileStorageService;
import cn.miyf.infrastructure.storage.StoragePathUtils;
import cn.miyf.infrastructure.storage.StoredFile;
import cn.miyf.oss.bean.dto.FileUploadCommand;
import cn.miyf.oss.bean.entity.SysResourceIndexEntity;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.enums.FileOperationType;
import cn.miyf.oss.repository.mapper.SysResourceIndexMapper;
import cn.miyf.oss.security.FileAccessPermissionCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

/**
 * 文件资源：资源索引维护 + 按 ID 打开流与访问校验。
 * 读文件时可结合控制器侧签名校验结果（{@link #assertReadable(Long, boolean)}）放行。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class FileResourceApplicationService {

    /**
     * 压缩时图片最长边像素上限。
     */
    private static final int COMPRESS_MAX_SIDE = 1280;

    /**
     * JPEG 压缩质量（约 0.78）。
     */
    private static final float JPEG_COMPRESS_QUALITY = 0.78f;

    private final FileStorageService fileStorageService;
    private final SysResourceIndexMapper sysResourceIndexMapper;
    private final FileStorageProperties fileStorageProperties;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final FileAccessPermissionCache fileAccessPermissionCache;

    /**
     * 存储并登记资源索引，返回 {@code /r/{id}.jpg} 等带后缀 URL。
     * <p>
     * 当 {@link FileUploadCommand#isCompress()} 为 true 且 Content-Type 为图片时，
     * 会先按最长边 {@value #COMPRESS_MAX_SIDE}、JPEG 质量约 {@value #JPEG_COMPRESS_QUALITY}
     * 压缩（含透明通道的 PNG 等会铺白底后转 JPEG）再写入存储；压缩失败或未缩小则回退原图上传。
     *
     * @param inputStream  内容流
     * @param size         字节数
     * @param contentType  Content-Type
     * @param originalName 原始名
     * @param command      业务参数
     * @return 上传结果
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Transactional
    public UploadedFileVo store(InputStream inputStream,
                                long size,
                                String contentType,
                                String originalName,
                                FileUploadCommand command) {
        if (command == null || !StringUtils.hasText(command.getAppCode())) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "appCode 不能为空");
        }
        String appCode;
        try {
            appCode = StoragePathUtils.requireAppCode(command.getAppCode());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_FILE, ex.getMessage());
        }
        long fileId = snowflakeIdGenerator.nextId();
        String path = StoragePathUtils.buildStoragePath(
                fileStorageProperties.getPathNamespace(), appCode, fileId);

        InputStream storeStream = inputStream;
        long storeSize = size;
        String storeContentType = contentType;
        // 需要压缩的图片先读入内存，便于失败时回退原字节
        if (command.isCompress() && isImageContentType(contentType)) {
            byte[] originalBytes;
            try {
                originalBytes = inputStream.readAllBytes();
            } catch (IOException ex) {
                throw new BusinessException(ErrorCode.INVALID_FILE, "读取上传文件失败");
            }
            CompressedImage compressed = tryCompressImage(originalBytes);
            if (compressed != null) {
                storeStream = new ByteArrayInputStream(compressed.bytes());
                storeSize = compressed.bytes().length;
                storeContentType = compressed.contentType();
            } else {
                storeStream = new ByteArrayInputStream(originalBytes);
                storeSize = originalBytes.length;
            }
        }

        StoredFile stored = fileStorageService.store(storeStream, storeSize, storeContentType, path);
        Long userId = SecurityUtils.currentPrincipal().map(AuthPrincipal::getId).orElse(null);
        FileAccessPermission permission = command.getAccessPermission() == null
                ? FileAccessPermission.OWNER
                : command.getAccessPermission();

        SysResourceIndexEntity entity = new SysResourceIndexEntity()
                .setMimeType(stored.contentType())
                .setFileName(trimName(originalName))
                .setFileSize(stored.size())
                .setCreateUser(userId)
                .setLastModifyUser(userId)
                .setCompress(command.isCompress())
                .setPath(stored.path())
                .setTemp(command.isTemp())
                .setSource(trimSource(command.getSource()))
                .setMd5(stored.md5())
                .setAppCode(appCode)
                .setAccessPermission(permission);
        entity.setId(fileId);
        sysResourceIndexMapper.insert(entity);

        return new UploadedFileVo()
                .setId(fileId)
                .setPath(stored.path())
                .setUrl(StoragePathUtils.publicResourceUrl(
                        fileStorageProperties.getBaseUrl(), fileId, stored.contentType()))
                .setContentType(stored.contentType())
                .setSize(stored.size())
                .setMd5(stored.md5())
                .setAppCode(appCode)
                .setAccessPermission(permission);
    }

    /**
     * 按文件 ID 加载资源索引。
     *
     * @param fileId 文件 ID
     * @return 元数据
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public SysResourceIndexEntity requireMeta(Long fileId) {
        if (fileId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        SysResourceIndexEntity entity = sysResourceIndexMapper.selectById(fileId);
        if (entity == null || !StringUtils.hasText(entity.getPath())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return entity;
    }

    /**
     * 校验当前主体是否可对资源执行指定操作。
     *
     * @param fileId    文件 ID
     * @param operation 操作
     * @return true 表示允许
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public boolean canAccess(Long fileId, FileOperationType operation) {
        if (operation == FileOperationType.UPLOAD) {
            return SecurityUtils.currentPrincipal().isPresent();
        }
        SysResourceIndexEntity meta = requireMeta(fileId);
        return canAccess(meta, operation);
    }

    /**
     * 校验当前主体是否可对资源执行指定操作。
     * <p>
     * 读取类：签名 URL（控制器校验）/ Redis 临时授权、上传人、公共文件；管理员放行。
     *
     * @param meta      资源
     * @param operation 操作
     * @return true 表示允许
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public boolean canAccess(SysResourceIndexEntity meta, FileOperationType operation) {
        if (meta == null || meta.getId() == null) {
            return false;
        }
        FileAccessPermission permission = meta.getAccessPermission() == null
                ? FileAccessPermission.OWNER
                : meta.getAccessPermission();
        if (permission == FileAccessPermission.DENY) {
            return false;
        }
        AuthPrincipal principal = SecurityUtils.currentPrincipal().orElse(null);
        boolean admin = principal != null && principal.getType() == PrincipalType.ADMIN;
        if (admin) {
            return true;
        }
        return switch (operation) {
            case READ, DOWNLOAD -> {
                if (permission == FileAccessPermission.PUBLIC) {
                    yield true;
                }
                if (permission == FileAccessPermission.AUTHENTICATED) {
                    yield principal != null;
                }
                if (permission == FileAccessPermission.ADMIN) {
                    yield false;
                }
                // OWNER：创建人或临时授权
                if (principal != null && principal.getId() != null
                        && principal.getId().equals(meta.getCreateUser())) {
                    yield true;
                }
                yield fileAccessPermissionCache.hasAccess(meta.getId());
            }
            case UPDATE, DELETE, CONFIRM -> principal != null && principal.getId() != null
                    && principal.getId().equals(meta.getCreateUser());
            case UPLOAD -> principal != null;
        };
    }

    /**
     * 读取前鉴权：不通过则抛禁止访问。
     * 委托 {@link #assertReadable(Long, boolean)}，默认不按签名放行。
     *
     * @param fileId 文件 ID
     * @throws BusinessException 文件不存在或无权访问
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void assertReadable(Long fileId) {
        assertReadable(fileId, false);
    }

    /**
     * 读取前鉴权。
     * 当控制器已校验通过 URL 上的 exp/sig 时，仅确认资源存在即可放行，不再要求登录主体。
     *
     * @param fileId         文件 ID
     * @param signedAccessOk 请求携带的 exp/sig 是否已校验通过
     * @throws BusinessException 文件不存在或无权访问
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public void assertReadable(Long fileId, boolean signedAccessOk) {
        if (signedAccessOk) {
            requireMeta(fileId);
            return;
        }
        SysResourceIndexEntity meta = requireMeta(fileId);
        if (!canAccess(meta, FileOperationType.READ)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该文件");
        }
    }

    /**
     * 按文件 ID 打开内容流（调用方关闭）。
     *
     * @param fileId 文件 ID
     * @return 输入流
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public InputStream open(Long fileId) {
        SysResourceIndexEntity meta = requireMeta(fileId);
        return fileStorageService.open(meta.getPath());
    }

    /**
     * 判断 Content-Type 是否为图片。
     *
     * @param contentType Content-Type
     * @return true 表示图片
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    private static boolean isImageContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.trim().toLowerCase(Locale.ROOT).startsWith("image/");
    }

    /**
     * 尝试压缩图片：最长边限制 {@value #COMPRESS_MAX_SIDE}，统一输出 JPEG（质量约 {@value #JPEG_COMPRESS_QUALITY}）；
     * PNG 等含透明通道时先铺白底再编码。失败或结果未缩小则返回 {@code null}，由调用方回退原图。
     *
     * @param originalBytes 原始字节
     * @return 压缩结果；无法压缩时为 null
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    private static CompressedImage tryCompressImage(byte[] originalBytes) {
        if (originalBytes == null || originalBytes.length == 0) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (source == null) {
                return null;
            }
            BufferedImage scaled = scaleToMaxSide(source, COMPRESS_MAX_SIDE);
            byte[] jpegBytes = encodeJpeg(scaled, JPEG_COMPRESS_QUALITY);
            // 未变小则回退，避免无意义改写 Content-Type
            if (jpegBytes.length == 0 || jpegBytes.length >= originalBytes.length) {
                return null;
            }
            return new CompressedImage(jpegBytes, "image/jpeg");
        } catch (Exception ignored) {
            // 任意编解码异常均回退原图，保证上传不因压缩失败
            return null;
        }
    }

    /**
     * 将图片最长边限制在 {@code maxSide} 以内，等比缩放；无需缩放时返回原图。
     *
     * @param source  原图
     * @param maxSide 最长边像素
     * @return 缩放后的图或原图
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    private static BufferedImage scaleToMaxSide(BufferedImage source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= maxSide) {
            return source;
        }
        double scale = (double) maxSide / (double) longest;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        // 保留 ARGB，透明像素在 JPEG 编码阶段再铺白底
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 将图片编码为 JPEG；非 RGB 或含透明时先绘制到白底 RGB 画布。
     *
     * @param image   源图
     * @param quality 压缩质量 0~1
     * @return JPEG 字节
     * @throws IOException 编码失败
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG ImageWriter unavailable");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            if (imageOutput == null) {
                throw new IOException("ImageOutputStream unavailable");
            }
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    /**
     * 压缩后的图片字节与 Content-Type。
     *
     * @param bytes       内容
     * @param contentType MIME
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    private record CompressedImage(byte[] bytes, String contentType) {
    }

    private static String trimName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return null;
        }
        String name = originalName.trim();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private static String trimSource(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String value = source.trim();
        return value.length() > 64 ? value.substring(0, 64) : value;
    }
}
