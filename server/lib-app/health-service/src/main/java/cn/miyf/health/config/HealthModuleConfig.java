package cn.miyf.health.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 健康模块配置注册。
 *
 * @author XieMingJie
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Configuration
@EnableConfigurationProperties(HealthProperties.class)
public class HealthModuleConfig {

    /**
     * 华为 OAuth / Health Kit HTTP 客户端（带超时）。
     *
     * @return RestClient
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Bean(name = "huaweiRestClient")
    public RestClient huaweiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        return RestClient.builder().requestFactory(factory).build();
    }
}
