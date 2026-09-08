package cn.miyf.config;

import cn.miyf.infrastructure.search.ElasticsearchSearchClient;
import cn.miyf.infrastructure.search.NoopSearchClient;
import cn.miyf.infrastructure.search.SearchClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索模块 Bean 装配。
 * 默认关闭 Elasticsearch，注入 {@link NoopSearchClient}，保证无 ES 进程时应用仍可启动；
 * 当 {@code app.search.enabled=true} 时创建 Rest5Client、官方 ElasticsearchClient 与业务门面。
 * Spring Boot 默认 ES 自动配置已在启动类排除，由本配置按开关显式装配。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Configuration
@EnableConfigurationProperties(SearchAppProperties.class)
public class SearchConfig {

    /**
     * 注册搜索关闭时的空实现。
     * matchIfMissing=true：未配置 enabled 时同样走 Noop。
     *
     * @param properties 搜索配置
     * @return Noop 客户端
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "false", matchIfMissing = true)
    public SearchClient noopSearchClient(SearchAppProperties properties) {
        return new NoopSearchClient(properties);
    }

    /**
     * 创建 Elasticsearch Rest5 低层客户端（仅启用时）。
     * 使用 HttpClient 5 超时与可选 Basic 认证；destroyMethod=close 在容器销毁时释放连接。
     *
     * @param properties 搜索配置（uris / 超时 / 账号）
     * @return Rest5Client
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    public Rest5Client elasticsearchRest5Client(SearchAppProperties properties) {
        List<URI> uris = new ArrayList<>();
        for (String uri : properties.getUris()) {
            if (StringUtils.hasText(uri)) {
                uris.add(URI.create(uri.trim()));
            }
        }
        if (uris.isEmpty()) {
            throw new IllegalStateException("app.search.uris 不能为空");
        }
        var builder = Rest5Client.builder(uris.toArray(URI[]::new));
        // Rest5 将连接超时与响应超时拆开配置
        builder.setConnectionConfigCallback(cc -> cc
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs())));
        builder.setRequestConfigCallback(rc -> rc
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getSocketTimeoutMs())));
        // 仅在配置了用户名时挂载凭证，避免空认证干扰无安全集群
        if (StringUtils.hasText(properties.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            char[] password = properties.getPassword() == null
                    ? new char[0]
                    : properties.getPassword().toCharArray();
            credentialsProvider.setCredentials(
                    new AuthScope(null, -1),
                    new UsernamePasswordCredentials(properties.getUsername(), password));
            builder.setHttpClientConfigCallback(http ->
                    http.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    /**
     * 创建官方 Java API 客户端。
     * 复用应用内 ObjectMapper，保证 Instant 等类型序列化与业务侧一致。
     *
     * @param rest5Client  Rest5 传输层
     * @param objectMapper 全局 Jackson
     * @return ElasticsearchClient
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(ElasticsearchClient.class)
    public ElasticsearchClient elasticsearchClient(Rest5Client rest5Client, ObjectMapper objectMapper) {
        return new ElasticsearchClient(new Rest5ClientTransport(rest5Client, new JacksonJsonpMapper(objectMapper)));
    }

    /**
     * 注册启用状态下的搜索门面实现。
     *
     * @param elasticsearchClient ES 客户端
     * @param properties          搜索配置（索引前缀等）
     * @return SearchClient
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    public SearchClient elasticsearchSearchClient(ElasticsearchClient elasticsearchClient,
                                                  SearchAppProperties properties) {
        return new ElasticsearchSearchClient(elasticsearchClient, properties);
    }
}
