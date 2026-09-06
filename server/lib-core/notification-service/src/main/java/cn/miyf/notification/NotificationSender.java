package cn.miyf.notification;

/**
 * 通知发送 SPI：邮件 / 短信 / 站内信实现此接口。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public interface NotificationSender {

    NotificationChannel channel();

    boolean supports(NotificationChannel channel);

    void send(NotificationMessage message);
}
