package cn.miyf.auth.config;

import cn.miyf.auth.infrastructure.wx.WxAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 微信登录相关配置。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:20
 * @history 1.00 2026-09-04 17:20 XieMingJie Created.
 */
@Configuration
@EnableConfigurationProperties({WxAuthProperties.class})
public class WxAuthConfig {

    /**
     * 微信 code2session 用 RestClient（含连接/读超时）。
     * 标记 {@link Primary}，与健康模块 {@code huaweiRestClient} 并存时默认注入本 Bean。
     *
     * @return RestClient
     * @history 1.00 2026-09-04 17:20 XieMingJie Created.
     */
    @Bean(name = "wxRestClient")
    @Primary
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        return RestClient.builder().requestFactory(factory).build();
    }
}
