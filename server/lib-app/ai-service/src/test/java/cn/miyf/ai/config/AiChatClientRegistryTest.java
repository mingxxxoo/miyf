package cn.miyf.ai.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI provider 注册表单元测试：开关、密钥缺失跳过、主用/兜底顺序、模型回落。
 *
 * @author XieMingJie
 * @since 2026-10-30 17:30
 */
class AiChatClientRegistryTest {

    @Test
    void disabledRegistryIsUnavailable() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        properties.setProviders(Map.of("opencode", provider("https://opencode.ai/inference/openai/v1",
                "oc_sk_test", "deepseek-v4.1-flash")));

        AiChatClientRegistry registry = AiChatClientRegistry.create(properties);

        assertFalse(registry.isAvailable());
        assertTrue(registry.clients().isEmpty());
    }

    @Test
    void skipsProviderWithoutApiKey() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setProvider("opencode");
        properties.setProviders(Map.of("opencode", provider("https://opencode.ai/inference/openai/v1",
                "", "deepseek-v4.1-flash")));

        AiChatClientRegistry registry = AiChatClientRegistry.create(properties);

        assertFalse(registry.isAvailable());
        assertTrue(registry.clients().isEmpty());
    }

    @Test
    void ordersPrimaryThenFallback() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setProvider("opencode");
        properties.setFallback("backup");
        Map<String, AiProperties.Provider> providers = new LinkedHashMap<>();
        providers.put("backup", provider("https://api.example.com/v1",
                "sk-backup", "deepseek-v4.1-flash"));
        providers.put("opencode", provider("https://opencode.ai/inference/openai/v1",
                "oc_sk_test", "deepseek-v4.1-flash"));
        properties.setProviders(providers);

        AiChatClientRegistry registry = AiChatClientRegistry.create(properties);

        assertTrue(registry.isAvailable());
        assertEquals(2, registry.clients().size());
        assertEquals("opencode", registry.clients().get(0).provider());
        assertEquals("deepseek-v4.1-flash", registry.clients().get(0).model());
        assertEquals("backup", registry.clients().get(1).provider());
    }

    @Test
    void fallsBackToDefaultModel() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setModel("deepseek-v4.1-flash");
        properties.setProvider("backup");
        properties.setProviders(Map.of("backup", provider("https://api.example.com/v1",
                "sk-backup", null)));

        AiChatClientRegistry registry = AiChatClientRegistry.create(properties);

        assertTrue(registry.isAvailable());
        assertEquals("deepseek-v4.1-flash", registry.clients().get(0).model());
    }

    private AiProperties.Provider provider(String baseUrl, String apiKey, String model) {
        AiProperties.Provider provider = new AiProperties.Provider();
        provider.setBaseUrl(baseUrl);
        provider.setApiKey(apiKey);
        provider.setModel(model);
        return provider;
    }
}
