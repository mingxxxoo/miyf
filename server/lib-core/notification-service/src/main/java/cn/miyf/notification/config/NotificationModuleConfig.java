package cn.miyf.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务模块配置注册。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationModuleConfig {
}
