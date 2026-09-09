package cn.miyf.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 示例心跳任务（演示注册、启停与调度）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HeartbeatJob {

    public static final String CODE = "system.heartbeat";

    private final JobRegistry jobRegistry;

    @MiyfJob(code = CODE, name = "系统心跳", description = "每 5 分钟打点，确认调度可用")
    @Scheduled(cron = "0 */5 * * * *")
    public void heartbeat() {
        if (!jobRegistry.shouldRun(CODE)) {
            return;
        }
        jobRegistry.markStart(CODE);
        try {
            log.debug("job system.heartbeat tick");
            jobRegistry.markSuccess(CODE);
        } catch (Exception ex) {
            jobRegistry.markFailed(CODE, ex.getMessage());
            throw ex;
        }
    }
}
