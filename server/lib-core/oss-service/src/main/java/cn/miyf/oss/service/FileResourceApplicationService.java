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

import java.io.InputStream;

/**
 * 文件资源：资源索引维护 + 按 ID 打开流与访问校验。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Service
@RequiredArgsConstructor
public class FileResourceApplicationService {

    private final FileStorageService fileStorageService;
    private final SysResourceIndexMapper sysResourceIndexMapper;
    private final FileStorageProperties fileStorageProperties;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final FileAccessPermissionCache fileAccessPermissionCache;

    /**
     * 存储并登记资源索引，返回 {@code /r/{id}.jpg} 等带后缀 URL。
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
        StoredFile stored = fileStorageService.store(inputStream, size, contentType, path);
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
     * 读取类：Redis 临时授权、上传人、公共文件；管理员放行。
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
     *
     * @param fileId 文件 ID
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public void assertReadable(Long fileId) {
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
