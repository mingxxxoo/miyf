package cn.miyf.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 应用配置开关与属性绑定。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Configuration
@EnableConfigurationProperties(RedisAppProperties.class)
public class RedisConfig {
}
