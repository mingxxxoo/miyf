package cn.miyf.health.job;

import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.service.HealthSyncApplicationService;
import cn.miyf.job.JobRegistry;
import cn.miyf.job.MiyfJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康数据源定时同步：遍历 ACTIVE 且支持远程拉取的绑定，按 lastSyncTime 增量拉取。
 * <p>
 * 可在系统任务控制台启停 / 手动触发；默认每小时整点后 15 分执行。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthSyncJob {

    public static final String CODE = "health.provider.sync";

    private final JobRegistry jobRegistry;
    private final HealthSyncApplicationService healthSyncApplicationService;
    private final HealthProperties healthProperties;

    
    /**
     * 定时同步全部可远程拉取的 ACTIVE 绑定；按 lastSyncTime 重叠 1h 增量拉取。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @MiyfJob(code = CODE, name = "健康数据源同步", description = "按 ACTIVE 绑定增量同步远程健康数据")
    @Scheduled(cron = "${app.health.sync.cron:0 15 * * * *}")
    public void syncAll() {
        if (!jobRegistry.shouldRun(CODE)) {
            return;
        }
        jobRegistry.markStart(CODE);
        if (!healthProperties.isEnabled()) {
            log.debug("job {} skipped: health module disabled", CODE);
            jobRegistry.markSuccess(CODE);
            return;
        }
        try {
            List<HealthProviderBindingEntity> bindings = healthSyncApplicationService.listActiveRemoteBindings();
            int ok = 0;
            int fail = 0;
            List<String> errors = new ArrayList<>();
            for (HealthProviderBindingEntity binding : bindings) {
                try {
                    HealthSyncRequestDto dto = new HealthSyncRequestDto()
                            .setSubjectId(String.valueOf(binding.getSubjectId()));
                    if (binding.getLastSyncTime() != null) {
                        // 重叠窗口，避免边界漏数（入库幂等）
                        dto.setFrom(binding.getLastSyncTime().minus(Duration.ofHours(1)));
                    }
                    dto.setTo(Instant.now());
                    HealthSyncRunEntity run = healthSyncApplicationService.sync(binding.getProviderCode(), dto);
                    if ("FAILED".equals(run.getStatus())) {
                        fail++;
                        errors.add(binding.getProviderCode() + "/" + binding.getSubjectId()
                                + ": " + run.getErrorMessage());
                    } else {
                        ok++;
                    }
                } catch (Exception ex) {
                    fail++;
                    errors.add(binding.getProviderCode() + "/" + binding.getSubjectId()
                            + ": " + ex.getMessage());
                    log.warn("health sync failed provider={} subject={}: {}",
                            binding.getProviderCode(), binding.getSubjectId(), ex.getMessage());
                }
            }
            log.info("job {} done: bindings={} ok={} fail={}", CODE, bindings.size(), ok, fail);
            if (fail > 0 && ok == 0 && !bindings.isEmpty()) {
                jobRegistry.markFailed(CODE, truncate(String.join("; ", errors), 500));
            } else {
                jobRegistry.markSuccess(CODE);
                if (fail > 0) {
                    log.warn("job {} partial failures: {}", CODE, String.join("; ", errors));
                }
            }
        } catch (Exception ex) {
            jobRegistry.markFailed(CODE, ex.getMessage());
            throw ex;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
