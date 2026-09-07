package cn.miyf.notification;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.notification.dto.NotifySendDto;
import cn.miyf.notification.dto.NotifyTemplateSaveDto;
import cn.miyf.notification.entity.SysNotifySendLogEntity;
import cn.miyf.notification.entity.SysNotifyTemplateEntity;
import cn.miyf.notification.repository.mapper.SysNotifySendLogMapper;
import cn.miyf.notification.repository.mapper.SysNotifyTemplateMapper;
import cn.miyf.notification.vo.NotifySendLogVo;
import cn.miyf.notification.vo.NotifyTemplateVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知门面：渠道路由、模板 CRUD、发送流水。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class NotificationApplicationService {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");
    private static final int SEND_LOG_LIMIT_MAX = 200;

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);
    private final InboxNotificationSender inboxNotificationSender;
    private final SysNotifyTemplateMapper templateMapper;
    private final SysNotifySendLogMapper sendLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public NotificationApplicationService(List<NotificationSender> senderList,
                                          InboxNotificationSender inboxNotificationSender,
                                          SysNotifyTemplateMapper templateMapper,
                                          SysNotifySendLogMapper sendLogMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.inboxNotificationSender = inboxNotificationSender;
        this.templateMapper = templateMapper;
        this.sendLogMapper = sendLogMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        List<NotificationSender> ordered = new ArrayList<>(senderList);
        AnnotationAwareOrderComparator.sort(ordered);
        for (NotificationSender sender : ordered) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (sender.supports(channel)) {
                    senders.putIfAbsent(channel, sender);
                }
            }
        }
        senders.put(NotificationChannel.INBOX, inboxNotificationSender);
    }

    public void send(NotificationMessage message) {
        sendInternal(message, null);
    }

    public void send(NotifySendDto dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求体不能为空");
        }
        if (!StringUtils.hasText(dto.getTo())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接收人不能为空");
        }
        Long templateId = null;
        String title = dto.getTitle();
        String content = dto.getContent();
        NotificationChannel channel;
        if (StringUtils.hasText(dto.getTemplateCode())) {
            SysNotifyTemplateEntity template = templateMapper.selectByCode(
                    dto.getTemplateCode().trim().toLowerCase(Locale.ROOT));
            if (template == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "通知模板不存在: " + dto.getTemplateCode());
            }
            if (!"ENABLED".equalsIgnoreCase(template.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "通知模板已停用");
            }
            templateId = template.getId();
            channel = NotificationChannel.valueOf(template.getChannel().trim().toUpperCase(Locale.ROOT));
            Map<String, String> vars = dto.getVars() == null ? Map.of() : dto.getVars();
            title = render(template.getTitleTemplate(), vars);
            content = render(template.getContentTemplate(), vars);
        } else {
            if (!StringUtils.hasText(dto.getChannel())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "通知渠道不能为空");
            }
            channel = NotificationChannel.valueOf(dto.getChannel().trim().toUpperCase(Locale.ROOT));
            title = title == null ? "" : title;
            content = content == null ? "" : content;
        }
        sendInternal(new NotificationMessage()
                .setChannel(channel)
                .setTo(dto.getTo().trim())
                .setTitle(title)
                .setContent(content), templateId);
    }

    private void sendInternal(NotificationMessage message, Long templateId) {
        if (message == null || message.getChannel() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知渠道不能为空");
        }
        NotificationSender sender = senders.get(message.getChannel());
        if (sender == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未注册通知渠道: " + message.getChannel());
        }
        Instant now = Instant.now();
        SysNotifySendLogEntity log = new SysNotifySendLogEntity()
                .setTemplateId(templateId)
                .setChannel(message.getChannel().name())
                .setToKey(StringUtils.hasText(message.getTo()) ? message.getTo().trim() : "")
                .setTitle(message.getTitle() == null ? "" : message.getTitle())
                .setContent(message.getContent());
        log.setId(snowflakeIdGenerator.nextId());
        log.setCreateTime(now);
        log.setLastModifyTime(now);
        try {
            sender.send(message);
            log.setStatus("SUCCESS");
            sendLogMapper.insert(log);
        } catch (RuntimeException ex) {
            log.setStatus("FAILED");
            log.setError(truncate(ex.getMessage(), 1000));
            sendLogMapper.insert(log);
            throw ex;
        }
    }

    public List<InboxItem> listInbox(String userKey, Integer limit) {
        return inboxNotificationSender.listByUser(userKey, limit);
    }

    public InboxItem markRead(Long id, String userKey) {
        return inboxNotificationSender.markRead(id, userKey);
    }

    public List<NotifyTemplateVo> listTemplates(String channel, String status) {
        return templateMapper.selectList(Wrappers.<SysNotifyTemplateEntity>lambdaQuery()
                        .eq(StringUtils.hasText(channel), SysNotifyTemplateEntity::getChannel,
                                channel.trim().toUpperCase(Locale.ROOT))
                        .eq(StringUtils.hasText(status), SysNotifyTemplateEntity::getStatus,
                                status.trim().toUpperCase(Locale.ROOT))
                        .orderByAsc(SysNotifyTemplateEntity::getCode))
                .stream()
                .map(this::toTemplateVo)
                .toList();
    }

    @Transactional
    public NotifyTemplateVo createTemplate(NotifyTemplateSaveDto dto) {
        String code = normalizeCode(dto.getCode());
        if (templateMapper.selectByCode(code) != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板编码已存在");
        }
        Instant now = Instant.now();
        SysNotifyTemplateEntity entity = fromTemplateDto(dto, code);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        templateMapper.insert(entity);
        return toTemplateVo(entity);
    }

    @Transactional
    public NotifyTemplateVo updateTemplate(Long id, NotifyTemplateSaveDto dto) {
        SysNotifyTemplateEntity entity = requireTemplate(id);
        String code = normalizeCode(dto.getCode());
        SysNotifyTemplateEntity byCode = templateMapper.selectByCode(code);
        if (byCode != null && !Objects.equals(byCode.getId(), id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板编码已存在");
        }
        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setChannel(normalizeChannel(dto.getChannel()));
        entity.setTitleTemplate(dto.getTitleTemplate().trim());
        entity.setContentTemplate(dto.getContentTemplate().trim());
        entity.setStatus(normalizeStatus(dto.getStatus()));
        entity.setLastModifyTime(Instant.now());
        templateMapper.updateById(entity);
        return toTemplateVo(entity);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        requireTemplate(id);
        templateMapper.deleteById(id);
    }

    public List<NotifySendLogVo> listSendLogs(String channel, String status, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, SEND_LOG_LIMIT_MAX);
        return sendLogMapper.selectList(Wrappers.<SysNotifySendLogEntity>lambdaQuery()
                        .eq(StringUtils.hasText(channel), SysNotifySendLogEntity::getChannel,
                                channel.trim().toUpperCase(Locale.ROOT))
                        .eq(StringUtils.hasText(status), SysNotifySendLogEntity::getStatus,
                                status.trim().toUpperCase(Locale.ROOT))
                        .orderByDesc(SysNotifySendLogEntity::getCreateTime)
                        .last("LIMIT " + size))
                .stream()
                .map(this::toSendLogVo)
                .toList();
    }

    private SysNotifyTemplateEntity fromTemplateDto(NotifyTemplateSaveDto dto, String code) {
        return new SysNotifyTemplateEntity()
                .setCode(code)
                .setName(dto.getName().trim())
                .setChannel(normalizeChannel(dto.getChannel()))
                .setTitleTemplate(dto.getTitleTemplate().trim())
                .setContentTemplate(dto.getContentTemplate().trim())
                .setStatus(normalizeStatus(dto.getStatus()));
    }

    private SysNotifyTemplateEntity requireTemplate(Long id) {
        SysNotifyTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知模板不存在");
        }
        return entity;
    }

    private NotifyTemplateVo toTemplateVo(SysNotifyTemplateEntity entity) {
        return new NotifyTemplateVo()
                .setId(entity.getId())
                .setCode(entity.getCode())
                .setName(entity.getName())
                .setChannel(entity.getChannel())
                .setTitleTemplate(entity.getTitleTemplate())
                .setContentTemplate(entity.getContentTemplate())
                .setStatus(entity.getStatus())
                .setCreateTime(entity.getCreateTime())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    private NotifySendLogVo toSendLogVo(SysNotifySendLogEntity entity) {
        return new NotifySendLogVo()
                .setId(entity.getId())
                .setTemplateId(entity.getTemplateId())
                .setChannel(entity.getChannel())
                .setToKey(entity.getToKey())
                .setTitle(entity.getTitle())
                .setContent(entity.getContent())
                .setStatus(entity.getStatus())
                .setError(entity.getError())
                .setCreateTime(entity.getCreateTime());
    }

    static String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        if (vars == null || vars.isEmpty()) {
            return template;
        }
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板编码不能为空");
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeChannel(String channel) {
        if (!StringUtils.hasText(channel)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "渠道不能为空");
        }
        String c = channel.trim().toUpperCase(Locale.ROOT);
        NotificationChannel.valueOf(c);
        return c;
    }

    private static String normalizeStatus(String status) {
        String s = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!"ENABLED".equals(s) && !"DISABLED".equals(s)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态无效");
        }
        return s;
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return null;
        }
        return raw.length() <= max ? raw : raw.substring(0, max);
    }
}
