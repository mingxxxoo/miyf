package cn.miyf.kitchen.service;

import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.TokenVersionService;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.config.FileStorageProperties;
import cn.miyf.kitchen.bean.dto.UserProfileSaveDto;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.qo.UserPageQo;
import cn.miyf.kitchen.bean.vo.UserVo;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.oss.bean.dto.FileUploadCommand;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.service.FileResourceApplicationService;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 厨房用户应用服务：管理端列表/启停；个人端资料读写与头像上传。
 * 启停转为 DISABLED 时递增 JWT tokenVersion（bump 失败回滚）；
 * 头像仅允许相对 /r/ 或与 file-storage.base-url 同 host 的本站绝对 URL。
 *
 * @author XieMingJie
 * @since 2026-09-05
 * @history 1.00 2026-09-05 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService extends BaseApplicationService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ENABLED", "DISABLED");

    private final UserRepository userRepository;
    private final FileResourceApplicationService fileResourceApplicationService;
    private final TokenVersionService tokenVersionService;
    private final FileStorageProperties fileStorageProperties;

    /**
     * 管理端用户分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public PageResult<UserVo> pageAdmin(UserPageQo qo) {
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        long off = offset(page, rows);
        var records = userRepository.selectAdminPage(qo.getKeyword(), qo.getStatus(), off, rows).stream()
                .map(EntityConverters::toUser)
                .map(this::toVo)
                .toList();
        long total = userRepository.countAdminPage(qo.getKeyword(), qo.getStatus());
        return PageResult.of(records, total, page, rows);
    }

    /**
     * 管理端用户详情。
     *
     * @param id 用户 ID
     * @return VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public UserVo getAdmin(Long id) {
        return toVo(EntityConverters.toUser(requireById(userRepository, id, "用户不存在")));
    }

    /**
     * 管理端启停登录；仅在状态变为 DISABLED 时 bump JWT tokenVersion。
     *
     * @param id     用户 ID
     * @param status ENABLED / DISABLED
     * @return 更新后 VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Transactional
    public UserVo updateAdminStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 ENABLED 或 DISABLED");
        }
        UserEntity existing = requireById(userRepository, id, "用户不存在");
        String previous = existing.getStatus();
        userRepository.updateStatus(id, normalized);
        boolean wasDisabled = previous != null && "DISABLED".equalsIgnoreCase(previous.trim());
        if ("DISABLED".equals(normalized) && !wasDisabled) {
            tokenVersionService.bump(PrincipalType.USER, id);
        }
        return getAdmin(id);
    }

    /**
     * 当前用户资料。
     *
     * @param userId 用户 ID
     * @return VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    public UserVo getMe(Long userId) {
        return toVo(EntityConverters.toUser(requireById(userRepository, userId, "用户不存在")));
    }

    /**
     * 更新当前用户昵称 / 头像。
     *
     * @param userId 用户 ID
     * @param dto    资料
     * @return 更新后 VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Transactional
    public UserVo updateMe(Long userId, UserProfileSaveDto dto) {
        UserEntity entity = requireById(userRepository, userId, "用户不存在");
        boolean changed = false;
        if (StringUtils.hasText(dto.getNickname())) {
            String nickname = dto.getNickname().trim();
            if (!Objects.equals(nickname, entity.getNickname())) {
                entity.setNickname(nickname);
                changed = true;
            }
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            String avatar = normalizeAvatarUrl(dto.getAvatarUrl().trim());
            if (!Objects.equals(avatar, entity.getAvatarUrl())) {
                entity.setAvatarUrl(avatar);
                changed = true;
            }
        }
        if (changed) {
            userRepository.updateById(entity);
        }
        return toVo(EntityConverters.toUser(entity));
    }

    /**
     * 上传头像并写回用户表。
     *
     * @param userId 用户 ID
     * @param file   图片
     * @return 更新后 VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Transactional
    public UserVo uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "请选择头像文件");
        }
        requireById(userRepository, userId, "用户不存在");
        FileUploadCommand command = new FileUploadCommand()
                .setAppCode("kitchen")
                .setSource("user-avatar")
                .setTemp(false)
                .setCompress(true)
                .setAccessPermission(FileAccessPermission.PUBLIC);
        try (InputStream in = file.getInputStream()) {
            UploadedFileVo uploaded = fileResourceApplicationService.store(
                    in,
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    command
            );
            UserProfileSaveDto dto = new UserProfileSaveDto().setAvatarUrl(uploaded.getUrl());
            return updateMe(userId, dto);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取头像失败");
        }
    }

    private UserVo toVo(cn.miyf.kitchen.bean.model.User user) {
        return new UserVo()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setAvatarUrl(user.getAvatarUrl())
                .setPhone(user.getPhone())
                .setWechatId(user.getWechatId())
                // 管理端脱敏：仅保留前后各 2 位
                .setOpenId(maskOpenId(user.getOpenid()))
                .setStatus(user.getStatus())
                .setCreateTime(user.getCreateTime())
                .setLastLoginTime(null);
    }

    /**
     * 仅允许本站文件路径（/r/、/api/r/），拒绝外链与危险协议。
     * 绝对 URL 必须与 {@code app.file-storage.base-url} 同 host，再归一为相对 path（去掉 query）。
     *
     * @param raw 原始 URL
     * @return 规范化本站路径
     */
    String normalizeAvatarUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像地址不能为空");
        }
        String avatar = raw.trim();
        String lower = avatar.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的头像地址协议");
        }
        String path = extractLocalResourcePath(avatar);
        if (path != null) {
            return path;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "头像地址仅支持本站 /r/ 资源路径");
    }

    /**
     * 从相对或本站绝对 URL 提取 /r/ 路径（不含 query）。
     *
     * @param raw 原始地址
     * @return 本站路径；非本站资源返回 null
     */
    private String extractLocalResourcePath(String raw) {
        String candidate = raw;
        if (raw.toLowerCase(Locale.ROOT).startsWith("http://")
                || raw.toLowerCase(Locale.ROOT).startsWith("https://")) {
            try {
                URI uri = URI.create(raw);
                if (!isAllowedAvatarHost(uri.getHost())) {
                    return null;
                }
                candidate = uri.getPath();
            } catch (Exception ex) {
                return null;
            }
        }
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        int q = candidate.indexOf('?');
        if (q >= 0) {
            candidate = candidate.substring(0, q);
        }
        if (candidate.startsWith("/r/") || candidate.startsWith("/api/r/")) {
            return candidate;
        }
        return null;
    }

    /**
     * 绝对 URL 主机是否与文件存储 base-url 一致（忽略大小写）。
     */
    private boolean isAllowedAvatarHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String base = fileStorageProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            return false;
        }
        try {
            URI baseUri = URI.create(base.trim());
            String allowed = baseUri.getHost();
            return StringUtils.hasText(allowed)
                    && allowed.equalsIgnoreCase(host.trim());
        } catch (Exception ex) {
            return false;
        }
    }

    private static String maskOpenId(String openId) {
        if (!StringUtils.hasText(openId) || openId.length() < 6) {
            return openId;
        }
        return openId.substring(0, 2) + "***" + openId.substring(openId.length() - 2);
    }
}
