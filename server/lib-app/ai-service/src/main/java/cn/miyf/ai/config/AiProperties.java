package cn.miyf.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块配置（DashScope / 超时 / 降级开关）。
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
     * 默认模型；结构化任务使用稳定版，不用 latest。
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
}
