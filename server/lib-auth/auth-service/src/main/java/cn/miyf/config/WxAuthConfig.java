package cn.miyf.config;

import cn.miyf.infrastructure.wx.WxAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 微信登录相关配置。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:20
 */
@Configuration
@EnableConfigurationProperties({WxAuthProperties.class})
public class WxAuthConfig {

    /**
     * RestClient，用于调用微信 code2session。
     *
     * @return RestClient
     * @history 1.00 2026-09-04 17:20 XieMingJie Created.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
