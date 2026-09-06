package cn.miyf.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 通知渠道配置。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private final Mail mail = new Mail();
    private final Sms sms = new Sms();

    public Mail getMail() {
        return mail;
    }

    public Sms getSms() {
        return sms;
    }

    public static class Mail {
        /**
         * 是否启用真实 SMTP（需配置 spring.mail.*）
         */
        private boolean enabled = false;
        private String from = "noreply@miyf.local";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Sms {
        /**
         * 厂商适配器开关；未启用时走日志发送器
         */
        private boolean enabled = false;
        private String provider = "logging";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }
}
