package cn.miyf.ai.config;

import cn.miyf.ai.prompt.AiPromptTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * AI provider 注册表：按配置为每个 provider 构建独立的 ChatClient，并给出「主用 → 兜底」的调用顺序。
 *
 * <p>每个 provider 单独持有一个 OpenAI 兼容客户端，可对接任意 OpenAI 兼容端点；
 * 未配置 api-key 的 provider 视为不可用，直接跳过，不会发起请求。</p>
 *
 * @author XieMingJie
 * @since 2026-10-30
 * @history 1.00 2026-10-30 XieMingJie Created.
 */
public final class AiChatClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiChatClientRegistry.class);

    /**
     * 默认温度；各场景调用时会显式覆盖。
     */
    private static final double DEFAULT_TEMPERATURE = 0.2d;

    private final boolean enabled;

    private final List<AiChatClient> clients;

    private AiChatClientRegistry(boolean enabled, List<AiChatClient> clients) {
        this.enabled = enabled;
        this.clients = clients;
    }

    /**
     * 按配置构建注册表；总开关关闭或 provider 不可用时返回空列表，由调用方走降级。
     *
     * @param properties AI 配置
     * @return 注册表
     * @history 1.00 2026-10-30 XieMingJie Created.
     */
    public static AiChatClientRegistry create(AiProperties properties) {
        if (!properties.isEnabled()) {
            return new AiChatClientRegistry(false, List.of());
        }
        Map<String, AiProperties.Provider> providers = properties.getProviders() == null
                ? Map.of()
                : properties.getProviders();
        List<AiChatClient> clients = new ArrayList<>();
        for (String code : resolveChain(properties)) {
            AiProperties.Provider provider = providers.get(code);
            String model = resolveModel(provider, properties);
            if (!isReady(provider, model)) {
                log.warn("AI provider 未配置完整（base-url / api-key / model），跳过 provider={}", code);
                continue;
            }
            try {
                clients.add(build(code, provider, model, properties.getMaxTokens()));
            } catch (Exception ex) {
                log.error("AI provider 初始化失败 provider={} reason={}", code, ex.toString());
            }
        }
        if (clients.isEmpty()) {
            log.warn("AI 已开启但没有可用 provider，接口将返回降级产物；请检查 app.ai.providers 配置");
        }
        return new AiChatClientRegistry(true, List.copyOf(clients));
    }

    /**
     * @return 是否可调用模型（总开关打开且至少有一个 provider 就绪）
     * @history 1.00 2026-10-30 XieMingJie Created.
     */
    public boolean isAvailable() {
        return enabled && !clients.isEmpty();
    }

    /**
     * @return 按调用顺序排列的 provider 客户端（主用在前，兜底在后）
     * @history 1.00 2026-10-30 XieMingJie Created.
     */
    public List<AiChatClient> clients() {
        return clients;
    }

    /**
     * 解析调用链：主用 → 兜底，去空、去重。
     */
    private static List<String> resolveChain(AiProperties properties) {
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        if (StringUtils.hasText(properties.getProvider())) {
            chain.add(properties.getProvider().trim());
        }
        if (StringUtils.hasText(properties.getFallback())) {
            chain.add(properties.getFallback().trim());
        }
        return List.copyOf(chain);
    }

    private static boolean isReady(AiProperties.Provider provider, String model) {
        return provider != null
                && StringUtils.hasText(provider.getBaseUrl())
                && StringUtils.hasText(provider.getApiKey())
                && StringUtils.hasText(model);
    }

    /**
     * provider 未单独指定模型时，回落到 app.ai.model。
     */
    private static String resolveModel(AiProperties.Provider provider, AiProperties properties) {
        if (provider != null && StringUtils.hasText(provider.getModel())) {
            return provider.getModel().trim();
        }
        return StringUtils.hasText(properties.getModel()) ? properties.getModel().trim() : null;
    }

    private static AiChatClient build(String code, AiProperties.Provider provider, String model, int maxTokens) {
        String baseUrl = trimTrailingSlash(provider.getBaseUrl().trim());
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(provider.getApiKey().trim())
                .model(model)
                .temperature(DEFAULT_TEMPERATURE)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMillis(Math.max(1_000, provider.getTimeoutMs())))
                .maxRetries(Math.max(0, provider.getMaxRetries()));
        Map<String, String> headers = provider.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            // 部分网关（如 OpenCode Go）要求固定请求头，否则直接 400 拒绝
            builder.customHeaders(headers);
            log.info("AI provider 附加请求头 provider={} headers={}", code, headers.keySet());
        }
        OpenAiChatOptions options = builder.build();
        ChatClient chatClient = ChatClient.builder(OpenAiChatModel.builder().options(options).build())
                .defaultSystem(AiPromptTemplates.COMMON_SYSTEM_HEAD)
                .build();
        log.info("AI provider 就绪 provider={} model={} baseUrl={}", code, model, baseUrl);
        return new AiChatClient(code, model, chatClient);
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 单个 provider 的客户端与其模型编码。
     *
     * @param provider provider 编码
     * @param model    模型编码
     * @param client   ChatClient
     */
    public record AiChatClient(String provider, String model, ChatClient client) {
    }
}
