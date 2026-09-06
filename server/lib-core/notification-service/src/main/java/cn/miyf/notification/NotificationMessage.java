package cn.miyf.notification;

import java.util.Map;

/**
 * 待发送通知。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class NotificationMessage {

    private NotificationChannel channel;
    private String to;
    private String title;
    private String content;
    private Map<String, Object> extras;

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationMessage setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public String getTo() {
        return to;
    }

    public NotificationMessage setTo(String to) {
        this.to = to;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public NotificationMessage setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getContent() {
        return content;
    }

    public NotificationMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public NotificationMessage setExtras(Map<String, Object> extras) {
        this.extras = extras;
        return this;
    }
}
