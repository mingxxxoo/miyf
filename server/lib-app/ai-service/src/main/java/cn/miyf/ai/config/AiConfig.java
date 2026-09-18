package cn.miyf.ai.config;

import cn.miyf.ai.prompt.AiPromptTemplates;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 模块 Bean 注册：ChatClient、网页抓取客户端。
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
     * 默认 ChatClient；仅在 app.ai.enabled=true 且 Spring AI ChatClient.Builder 可用时创建。
     *
     * @param builder    Spring AI 自动配置的 Builder（OpenAI 协议 / DashScope 兼容模式）
     * @param properties AI 配置
     * @return ChatClient
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    @ConditionalOnBean(ChatClient.Builder.class)
    public ChatClient aiChatClient(ChatClient.Builder builder, AiProperties properties) {
        return builder
                .defaultSystem(AiPromptTemplates.COMMON_SYSTEM_HEAD)
                .defaultOptions(ChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(0.2d)
                        .maxTokens(properties.getMaxTokens()))
                .build();
    }
}
