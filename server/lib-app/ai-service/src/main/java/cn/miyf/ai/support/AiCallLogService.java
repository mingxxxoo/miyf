package cn.miyf.ai.support;

import cn.miyf.ai.bean.entity.AiCallLogEntity;
import cn.miyf.ai.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 调用审计日志写入。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    private static final Logger log = LoggerFactory.getLogger(AiCallLogService.class);

    private final AiCallLogRepository aiCallLogRepository;

    /**
     * 异步失败不影响主流程：写库异常仅记日志。
     *
     * @param entity 日志实体
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public void saveQuietly(AiCallLogEntity entity) {
        try {
            if (entity.getInputSummary() != null && entity.getInputSummary().length() > 500) {
                entity.setInputSummary(entity.getInputSummary().substring(0, 500));
            }
            if (StringUtils.hasText(entity.getErrorMessage()) && entity.getErrorMessage().length() > 500) {
                entity.setErrorMessage(entity.getErrorMessage().substring(0, 500));
            }
            aiCallLogRepository.insert(entity);
        } catch (Exception ex) {
            log.warn("写入 ai_call_log 失败: {}", ex.toString());
        }
    }
}
