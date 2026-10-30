package cn.miyf.ai.support;

import cn.miyf.ai.bean.entity.AiCallLogEntity;
import cn.miyf.ai.config.AiChatClientRegistry;
import cn.miyf.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 统一 ChatClient 结构化调用：按 provider 顺序（主用 → 兜底）调用、瞬时失败重试、落审计日志。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Component
@RequiredArgsConstructor
public class AiStructuredCaller {

    private static final Logger log = LoggerFactory.getLogger(AiStructuredCaller.class);

    /**
     * ai_call_log.model_name 列长度。
     */
    private static final int MODEL_NAME_MAX_LENGTH = 64;

    private final AiChatClientRegistry chatClientRegistry;
    private final AiProperties aiProperties;
    private final AiCallLogService aiCallLogService;
    private final ObjectMapper objectMapper;

    /**
     * @return 当前是否可调用模型
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public boolean isAvailable() {
        return aiProperties.isEnabled() && chatClientRegistry.isAvailable();
    }

    /**
     * 结构化调用。
     *
     * <p>按注册表顺序依次尝试 provider：单个 provider 内按瞬时失败策略重试，仍失败则切下一个 provider；
     * 全部失败时记录一条失败日志并抛出最后一个异常。</p>
     *
     * @param scene        场景编码
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param temperature  温度
     * @param retry        瞬时失败重试次数
     * @param type         输出类型
     * @param userId       用户
     * @param kitchenId    厨房
     * @param inputSummary 输入摘要
     * @param <T>          结果类型
     * @return 结构化结果
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public <T> T callEntity(String scene,
                            String systemPrompt,
                            String userPrompt,
                            double temperature,
                            int retry,
                            Class<T> type,
                            Long userId,
                            Long kitchenId,
                            String inputSummary) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI ChatClient 不可用");
        }
        long start = System.currentTimeMillis();
        RuntimeException last = null;
        for (AiChatClientRegistry.AiChatClient provider : chatClientRegistry.clients()) {
            try {
                return callWithProvider(provider, scene, systemPrompt, userPrompt, temperature, retry, type,
                        userId, kitchenId, inputSummary, start);
            } catch (RuntimeException ex) {
                last = ex;
                log.warn("AI 调用失败，尝试下一个 provider provider={} scene={} reason={}",
                        provider.provider(), scene, ex.toString());
            }
        }
        writeLog(scene, userId, kitchenId, inputSummary, null, null, null,
                System.currentTimeMillis() - start, false, null,
                last == null ? "无可用 provider" : last.getMessage());
        throw last == null ? new IllegalStateException("AI 无可用 provider") : last;
    }

    /**
     * 在单个 provider 上调用，按瞬时失败策略重试。
     */
    private <T> T callWithProvider(AiChatClientRegistry.AiChatClient provider,
                                   String scene,
                                   String systemPrompt,
                                   String userPrompt,
                                   double temperature,
                                   int retry,
                                   Class<T> type,
                                   Long userId,
                                   Long kitchenId,
                                   String inputSummary,
                                   long start) {
        int attempts = Math.max(0, retry) + 1;
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                var responseEntity = provider.client().prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .options(OpenAiChatOptions.builder()
                                .model(provider.model())
                                .temperature(temperature)
                                .maxTokens(aiProperties.getMaxTokens()))
                        .call()
                        .responseEntity(type);
                T entity = responseEntity.getEntity();
                ChatResponse chatResponse = responseEntity.getResponse();
                Integer promptTokens = null;
                Integer completionTokens = null;
                if (chatResponse != null && chatResponse.getMetadata() != null
                        && chatResponse.getMetadata().getUsage() != null) {
                    Usage usage = chatResponse.getMetadata().getUsage();
                    promptTokens = usage.getPromptTokens() == null ? null : usage.getPromptTokens().intValue();
                    completionTokens = usage.getCompletionTokens() == null ? null
                            : usage.getCompletionTokens().intValue();
                }
                writeLog(scene, userId, kitchenId, inputSummary, toJson(entity),
                        promptTokens, completionTokens, System.currentTimeMillis() - start, true,
                        modelName(provider), null);
                return entity;
            } catch (Exception ex) {
                last = ex instanceof RuntimeException re ? re : new IllegalStateException(ex);
                if (i + 1 < attempts && isTransient(ex)) {
                    log.warn("AI 调用瞬时失败，重试 provider={} scene={} attempt={}: {}",
                            provider.provider(), scene, i + 1, ex.toString());
                    continue;
                }
                throw last;
            }
        }
        throw last == null ? new IllegalStateException("AI 调用失败") : last;
    }

    private boolean isTransient(Exception ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return msg.contains("timeout")
                || msg.contains("timed out")
                || msg.contains("5xx")
                || msg.contains("503")
                || msg.contains("502")
                || msg.contains("504");
    }

    /**
     * 审计日志里的模型标识：provider:model，超出列长度时截断。
     */
    private String modelName(AiChatClientRegistry.AiChatClient provider) {
        String value = provider.provider() + ":" + provider.model();
        return value.length() <= MODEL_NAME_MAX_LENGTH ? value : value.substring(0, MODEL_NAME_MAX_LENGTH);
    }

    private void writeLog(String scene, Long userId, Long kitchenId, String inputSummary,
                          String outputJson, Integer promptTokens, Integer completionTokens,
                          long durationMs, boolean success, String modelName, String error) {
        AiCallLogEntity entity = new AiCallLogEntity()
                .setScene(scene)
                .setUserId(userId)
                .setKitchenId(kitchenId)
                .setModelName(modelName)
                .setInputSummary(inputSummary)
                .setOutputJson(outputJson)
                .setPromptTokens(promptTokens)
                .setCompletionTokens(completionTokens)
                .setDurationMs(durationMs)
                .setSuccess(success)
                .setErrorMessage(error)
                .setAdopted(null);
        aiCallLogService.saveQuietly(entity);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 降级包装：不可用或失败时返回 supplier 结果。
     *
     * @param availableCheck 可用性
     * @param action         主逻辑
     * @param fallback       降级
     * @param <T>            类型
     * @return 结果
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public <T> T withFallback(Supplier<Boolean> availableCheck, Supplier<T> action, Supplier<T> fallback) {
        if (!Boolean.TRUE.equals(availableCheck.get())) {
            return fallback.get();
        }
        try {
            return action.get();
        } catch (Exception ex) {
            log.warn("AI 调用失败，走降级: {}", ex.toString());
            return fallback.get();
        }
    }
}
