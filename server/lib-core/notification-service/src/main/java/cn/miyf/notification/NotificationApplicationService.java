package cn.miyf.notification;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知门面：按渠道路由到 {@link NotificationSender}。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class NotificationApplicationService {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);
    private final InboxNotificationSender inboxNotificationSender;

    public NotificationApplicationService(List<NotificationSender> senderList,
                                          InboxNotificationSender inboxNotificationSender) {
        this.inboxNotificationSender = inboxNotificationSender;
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
        if (message == null || message.getChannel() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知渠道不能为空");
        }
        NotificationSender sender = senders.get(message.getChannel());
        if (sender == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未注册通知渠道: " + message.getChannel());
        }
        sender.send(message);
    }

    public List<InboxItem> listInbox(String userKey, Integer limit) {
        return inboxNotificationSender.listByUser(userKey, limit);
    }

    public InboxItem markRead(Long id, String userKey) {
        return inboxNotificationSender.markRead(id, userKey);
    }
}
