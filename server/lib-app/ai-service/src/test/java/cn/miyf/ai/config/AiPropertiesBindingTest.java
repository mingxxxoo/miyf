package cn.miyf.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 配置绑定单元测试：校验 application.yml 中 provider / providers 的 kebab-case 键能正确绑定。
 *
 * @author XieMingJie
 * @since 2026-10-30 17:40
 */
class AiPropertiesBindingTest {

    @Test
    void bindsProvidersMapWithKebabCaseKeys() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("app.ai.enabled", "true");
        source.put("app.ai.provider", "opencode");
        source.put("app.ai.providers.opencode.base-url", "https://opencode.ai/inference/openai/v1");
        source.put("app.ai.providers.opencode.api-key", "oc_sk_test");
        source.put("app.ai.providers.opencode.model", "deepseek-v4.1-flash");
        source.put("app.ai.providers.opencode.timeout-ms", "30000");

        Binder binder = new Binder(new MapConfigurationPropertySource(source));
        AiProperties properties = binder.bind("app.ai", Bindable.of(AiProperties.class)).get();

        assertTrue(properties.isEnabled());
        assertEquals("opencode", properties.getProvider());
        assertEquals("", properties.getFallback());
        assertEquals(1, properties.getProviders().size());
        AiProperties.Provider opencode = properties.getProviders().get("opencode");
        assertNotNull(opencode);
        assertEquals("https://opencode.ai/inference/openai/v1", opencode.getBaseUrl());
        assertEquals("deepseek-v4.1-flash", opencode.getModel());
        assertEquals(30_000, opencode.getTimeoutMs());
    }
}
