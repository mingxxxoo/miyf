package cn.miyf.ai.support;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * ChatClient 请求参数类型测试：模型默认参数与调用参数合并后仍须是 OpenAiChatOptions，
 * 否则 OpenAiChatModel 内部的强转会失败。
 *
 * @author XieMingJie
 * @since 2026-10-30 17:45
 */
class ChatOptionsPassThroughTest {

    @Test
    void requestOptionsStayOpenAiTyped() {
        CapturingChatModel model = new CapturingChatModel();
        ChatClient client = ChatClient.builder(model).build();

        client.prompt()
                .user("ping")
                .options(OpenAiChatOptions.builder()
                        .model("openai-model")
                        .temperature(0.1d)
                        .maxTokens(128))
                .call()
                .chatResponse();

        ChatOptions captured = model.prompt.getOptions();
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, captured);
        assertEquals("openai-model", options.getModel());
        assertEquals(0.1d, options.getTemperature());
        assertEquals(128, options.getMaxTokens());
    }

    /**
     * 只记录 Prompt 的 ChatModel 桩；模拟 OpenAiChatModel 的默认参数类型。
     */
    private static final class CapturingChatModel implements ChatModel {

        private Prompt prompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.prompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage("{}"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.empty();
        }

        @Override
        public ChatOptions getOptions() {
            return OpenAiChatOptions.builder().model("default-model").temperature(0.2d).build();
        }
    }
}
