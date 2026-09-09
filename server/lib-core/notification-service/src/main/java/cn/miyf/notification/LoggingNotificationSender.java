package cn.miyf.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认日志发送器（未接真实邮件/短信厂商时落日志）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@Order(100)
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL || channel == NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("notification[logging] channel={} to={} title={} content={}",
                message.getChannel(), message.getTo(), message.getTitle(), message.getContent());
    }
}
