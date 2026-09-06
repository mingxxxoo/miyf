package cn.miyf.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 示例心跳任务（演示注册、启停与调度）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class HeartbeatJob {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatJob.class);
    public static final String CODE = "system.heartbeat";

    private final JobRegistry jobRegistry;

    public HeartbeatJob(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

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
