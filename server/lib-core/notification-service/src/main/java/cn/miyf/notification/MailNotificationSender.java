package cn.miyf.notification;

import cn.miyf.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMTP 邮件发送适配器（需 {@code app.notification.mail.enabled=true} 与 {@code spring.mail.*}）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "app.notification.mail", name = "enabled", havingValue = "true")
@ConditionalOnBean(JavaMailSender.class)
@Slf4j
@RequiredArgsConstructor
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;
    private final NotificationProperties notificationProperties;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        if (!StringUtils.hasText(message.getTo())) {
            throw new IllegalArgumentException("邮件收件人不能为空");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(notificationProperties.getMail().getFrom());
        mail.setTo(message.getTo().trim());
        mail.setSubject(message.getTitle() == null ? "" : message.getTitle());
        mail.setText(message.getContent() == null ? "" : message.getContent());
        javaMailSender.send(mail);
        log.info("mail sent to={}", message.getTo());
    }
}
