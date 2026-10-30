package cn.miyf.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 模块 Bean 注册：provider 客户端注册表、网页抓取客户端。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * 网页抓取 RestClient（独立超时，带 SSRF 防护由 WebPageFetcher 负责）。
     *
     * @param properties AI 配置
     * @return RestClient
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Bean(name = "aiWebRestClient")
    public RestClient aiWebRestClient(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getWebFetch().getConnectTimeoutMs());
        factory.setReadTimeout(properties.getWebFetch().getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * AI provider 注册表；按 app.ai.providers 逐个构建客户端，调用顺序为主用 → 兜底。
     *
     * <p>不再依赖 Spring AI 自动配置的 ChatClient.Builder，因此切换 provider 只需改配置，
     * 且总开关关闭时不会创建任何模型客户端。</p>
     *
     * @param properties AI 配置
     * @return 注册表
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Bean
    public AiChatClientRegistry aiChatClientRegistry(AiProperties properties) {
        return AiChatClientRegistry.create(properties);
    }
}
