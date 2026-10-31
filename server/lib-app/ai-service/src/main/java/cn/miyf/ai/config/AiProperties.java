package cn.miyf.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模块配置（模型 / 超时 / 降级开关）。
 *
 * <p>支持多 provider：{@code provider} 指定主用编码，{@code fallback} 指定兜底编码，
 * 具体端点与模型在 {@code providers} 中按编码配置；调用顺序为主用 → 兜底。</p>
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * 总开关；关闭时接口返回降级产物，不调用模型。
     */
    private boolean enabled = false;

    /**
     * 主用 provider 编码，对应 {@link #providers} 的 key。
     */
    private String provider = "opencode";

    /**
     * 兜底 provider 编码；主用未配置密钥或调用失败时使用，留空表示不兜底。
     */
    private String fallback = "dashscope";

    /**
     * provider 配置表：编码 → 端点与模型。
     */
    private Map<String, Provider> providers = new LinkedHashMap<>();

    /**
     * 默认模型；仅在 provider 未单独配置模型时兜底使用。
     */
    private String model = "qwen-plus";

    private int maxTokens = 4096;

    private final Scene chefDish = new Scene(0.2d, 30_000, 1);
    private final Scene orderDraft = new Scene(0.1d, 10_000, 0);
    private final Scene orderInsight = new Scene(0.3d, 15_000, 1);

    private final WebFetch webFetch = new WebFetch();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Scene getChefDish() {
        return chefDish;
    }

    public Scene getOrderDraft() {
        return orderDraft;
    }

    public Scene getOrderInsight() {
        return orderInsight;
    }

    public WebFetch getWebFetch() {
        return webFetch;
    }

    /**
     * 单场景调用参数。
     */
    public static class Scene {
        private double temperature;
        private int timeoutMs;
        private int retryOnTransient;

        public Scene() {
        }

        public Scene(double temperature, int timeoutMs, int retryOnTransient) {
            this.temperature = temperature;
            this.timeoutMs = timeoutMs;
            this.retryOnTransient = retryOnTransient;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getRetryOnTransient() {
            return retryOnTransient;
        }

        public void setRetryOnTransient(int retryOnTransient) {
            this.retryOnTransient = retryOnTransient;
        }
    }

    /**
     * 网页抓取约束。
     */
    public static class WebFetch {
        private int connectTimeoutMs = 5_000;
        private int readTimeoutMs = 15_000;
        private int maxRedirects = 3;
        private int maxChars = 8_000;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getMaxRedirects() {
            return maxRedirects;
        }

        public void setMaxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }
    }

    /**
     * 单个 provider 的端点与模型配置。
     *
     * <p>base-url 必须包含版本段（如 {@code https://opencode.ai/inference/openai/v1}），
     * OpenAI SDK 只在其后追加 {@code /chat/completions}，不会再补 {@code /v1}。</p>
     */
    public static class Provider {

        /**
         * OpenAI 兼容端点（含版本段）。
         */
        private String baseUrl;

        /**
         * 接口密钥；为空表示该 provider 未配置，按不可用处理。
         */
        private String apiKey = "";

        /**
         * 模型编码。
         */
        private String model;

        /**
         * 单次请求超时（毫秒）。
         */
        private int timeoutMs = 30_000;

        /**
         * SDK 层重试次数；瞬时重试由 AiStructuredCaller 统一控制，这里默认不重试。
         */
        private int maxRetries = 0;

        /**
         * 额外请求头：部分网关要求固定头（如 OpenCode Go 必带 {@code x-opencode-session}）。
         */
        private Map<String, String> headers = new LinkedHashMap<>();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers == null ? new LinkedHashMap<>() : headers;
        }
    }
}
