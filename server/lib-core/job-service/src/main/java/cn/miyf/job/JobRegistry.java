package cn.miyf.job;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.job.entity.SysJobStateEntity;
import cn.miyf.job.repository.mapper.SysJobStateMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描 {@link MiyfJob}、持久化启停状态，并支持手动触发。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class JobRegistry {

    private final Map<String, JobDescriptor> jobs = new LinkedHashMap<>();
    private final Map<String, Runnable> runners = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> forceRun = new ThreadLocal<>();
    private final ApplicationContext applicationContext;
    private final SysJobStateMapper sysJobStateMapper;

    public JobRegistry(ApplicationContext applicationContext, SysJobStateMapper sysJobStateMapper) {
        this.applicationContext = applicationContext;
        this.sysJobStateMapper = sysJobStateMapper;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void scan() {
        jobs.clear();
        runners.clear();
        Map<String, Boolean> persisted = loadPersistedEnabled();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(AopUtils.getTargetClass(bean));
            for (Method method : userClass.getDeclaredMethods()) {
                MiyfJob meta = method.getAnnotation(MiyfJob.class);
                if (meta == null) {
                    continue;
                }
                Method invocable = method;
                ReflectionUtils.makeAccessible(invocable);
                String cron = resolveCron(method);
                boolean enabled = persisted.getOrDefault(meta.code(), true);
                jobs.put(meta.code(), new JobDescriptor()
                        .setCode(meta.code())
                        .setName(meta.name().isBlank() ? meta.code() : meta.name())
                        .setDescription(meta.description())
                        .setBeanName(beanName)
                        .setMethodName(method.getName())
                        .setCron(cron)
                        .setEnabled(enabled)
                        .setLastStatus("IDLE"));
                Object target = bean;
                runners.put(meta.code(), () -> ReflectionUtils.invokeMethod(invocable, target));
                ensureStateRow(meta.code(), enabled);
            }
        }
    }

    public List<JobDescriptor> list() {
        Instant now = Instant.now();
        List<JobDescriptor> list = new ArrayList<>(jobs.size());
        for (JobDescriptor job : jobs.values()) {
            job.setNextRunTime(computeNextRunTime(job.getCron(), now));
            list.add(job);
        }
        return list;
    }

    private static Instant computeNextRunTime(String cron, Instant now) {
        if (!StringUtils.hasText(cron) || "fixedRate".equals(cron) || "fixedDelay".equals(cron)) {
            return null;
        }
        try {
            CronExpression expression = CronExpression.parse(cron);
            LocalDateTime next = expression.next(LocalDateTime.ofInstant(now, ZoneId.systemDefault()));
            return next == null ? null : next.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ex) {
            return null;
        }
    }

    public JobDescriptor require(String code) {
        JobDescriptor job = jobs.get(code);
        if (job == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在: " + code);
        }
        return job;
    }

    public boolean isEnabled(String code) {
        JobDescriptor job = jobs.get(code);
        return job == null || job.isEnabled();
    }

    /**
     * 定时调度入口判断：禁用则跳过；手动触发（force）始终执行。
     */
    public boolean shouldRun(String code) {
        if (Boolean.TRUE.equals(forceRun.get())) {
            return true;
        }
        return isEnabled(code);
    }

    public JobDescriptor setEnabled(String code, boolean enabled) {
        JobDescriptor job = require(code);
        job.setEnabled(enabled);
        SysJobStateEntity state = new SysJobStateEntity()
                .setCode(code)
                .setEnabled(enabled)
                .setLastModifyTime(Instant.now());
        if (sysJobStateMapper.selectById(code) == null) {
            sysJobStateMapper.insert(state);
        } else {
            sysJobStateMapper.updateById(state);
        }
        return job;
    }

    public JobDescriptor trigger(String code) {
        JobDescriptor job = require(code);
        Runnable runner = runners.get(code);
        if (runner == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务不可执行: " + code);
        }
        forceRun.set(true);
        markStart(code);
        try {
            runner.run();
            if ("RUNNING".equals(job.getLastStatus())) {
                markSuccess(code);
            }
            return job;
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            markFailed(code, cause.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "任务执行失败: " + cause.getMessage());
        } finally {
            forceRun.remove();
        }
    }

    public void markStart(String code) {
        JobDescriptor job = jobs.get(code);
        if (job == null) {
            return;
        }
        job.setLastStartedTime(Instant.now()).setLastStatus("RUNNING").setLastError(null);
    }

    public void markSuccess(String code) {
        JobDescriptor job = jobs.get(code);
        if (job == null) {
            return;
        }
        job.setLastFinishedTime(Instant.now()).setLastStatus("SUCCESS");
    }

    public void markFailed(String code, String error) {
        JobDescriptor job = jobs.get(code);
        if (job == null) {
            return;
        }
        job.setLastFinishedTime(Instant.now()).setLastStatus("FAILED").setLastError(error);
    }

    public Collection<JobDescriptor> all() {
        return jobs.values();
    }

    private Map<String, Boolean> loadPersistedEnabled() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (SysJobStateEntity row : sysJobStateMapper.selectList(null)) {
            map.put(row.getCode(), !Boolean.FALSE.equals(row.getEnabled()));
        }
        return map;
    }

    private void ensureStateRow(String code, boolean enabled) {
        if (sysJobStateMapper.selectById(code) != null) {
            return;
        }
        sysJobStateMapper.insert(new SysJobStateEntity()
                .setCode(code)
                .setEnabled(enabled)
                .setLastModifyTime(Instant.now()));
    }

    private static String resolveCron(Method method) {
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        if (scheduled == null) {
            return null;
        }
        if (StringUtils.hasText(scheduled.cron())) {
            return scheduled.cron();
        }
        if (scheduled.fixedRate() > 0 || scheduled.fixedRateString().length() > 0) {
            return "fixedRate";
        }
        if (scheduled.fixedDelay() > 0 || scheduled.fixedDelayString().length() > 0) {
            return "fixedDelay";
        }
        return null;
    }
}
