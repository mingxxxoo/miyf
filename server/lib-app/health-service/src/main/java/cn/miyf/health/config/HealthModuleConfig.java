package cn.miyf.health.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 健康模块配置注册。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Configuration
@EnableConfigurationProperties(HealthProperties.class)
public class HealthModuleConfig {
}
