package cn.miyf.auth.config;

import cn.miyf.auth.infrastructure.huawei.HuaweiAccountProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 华为账号登录配置绑定。HTTP 复用已有 {@code wxRestClient}，不另建客户端。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@Configuration
@EnableConfigurationProperties(HuaweiAccountProperties.class)
public class HuaweiAuthConfig {
}
