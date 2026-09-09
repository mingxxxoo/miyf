package cn.miyf.notification;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.notification.entity.SysInboxMessageEntity;
import cn.miyf.notification.repository.mapper.SysInboxMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 站内信发送器（落库）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@RequiredArgsConstructor
public class InboxNotificationSender implements NotificationSender {

    private final SysInboxMessageMapper sysInboxMessageMapper;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.INBOX;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.INBOX;
    }

    @Override
    public void send(NotificationMessage message) {
        String userKey = StringUtils.hasText(message.getTo()) ? message.getTo().trim() : "anonymous";
        SysInboxMessageEntity entity = new SysInboxMessageEntity()
                .setUserKey(userKey)
                .setTitle(message.getTitle() == null ? "" : message.getTitle())
                .setContent(message.getContent())
                .setReadFlag(false);
        sysInboxMessageMapper.insert(entity);
    }

    public List<InboxItem> listByUser(String userKey, Integer limit) {
        if (!StringUtils.hasText(userKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userKey 不能为空");
        }
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return sysInboxMessageMapper.selectList(new LambdaQueryWrapper<SysInboxMessageEntity>()
                        .eq(SysInboxMessageEntity::getUserKey, userKey.trim())
                        .orderByDesc(SysInboxMessageEntity::getCreateTime)
                        .last("LIMIT " + size))
                .stream()
                .map(this::toItem)
                .toList();
    }

    public InboxItem markRead(Long id, String userKey) {
        SysInboxMessageEntity entity = sysInboxMessageMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "站内信不存在");
        }
        if (StringUtils.hasText(userKey) && !userKey.trim().equals(entity.getUserKey())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该站内信");
        }
        if (!Boolean.TRUE.equals(entity.getReadFlag())) {
            entity.setReadFlag(true);
            sysInboxMessageMapper.updateById(entity);
        }
        return toItem(entity);
    }

    private InboxItem toItem(SysInboxMessageEntity entity) {
        return new InboxItem(
                String.valueOf(entity.getId()),
                entity.getUserKey(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreateTime(),
                Boolean.TRUE.equals(entity.getReadFlag())
        );
    }
}
