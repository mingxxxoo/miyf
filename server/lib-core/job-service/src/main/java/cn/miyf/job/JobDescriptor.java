package cn.miyf.job;

import java.time.Instant;

/**
 * 任务注册与最近执行信息。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class JobDescriptor {

    private String code;
    private String name;
    private String description;
    private String beanName;
    private String methodName;
    private String cron;
    private boolean enabled = true;
    private Instant lastStartedTime;
    private Instant lastFinishedTime;
    private String lastStatus;
    private String lastError;
    private Instant nextRunTime;

    public String getCode() {
        return code;
    }

    public JobDescriptor setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public JobDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public JobDescriptor setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getBeanName() {
        return beanName;
    }

    public JobDescriptor setBeanName(String beanName) {
        this.beanName = beanName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public JobDescriptor setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public String getCron() {
        return cron;
    }

    public JobDescriptor setCron(String cron) {
        this.cron = cron;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JobDescriptor setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Instant getLastStartedTime() {
        return lastStartedTime;
    }

    public JobDescriptor setLastStartedTime(Instant lastStartedTime) {
        this.lastStartedTime = lastStartedTime;
        return this;
    }

    public Instant getLastFinishedTime() {
        return lastFinishedTime;
    }

    public JobDescriptor setLastFinishedTime(Instant lastFinishedTime) {
        this.lastFinishedTime = lastFinishedTime;
        return this;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public JobDescriptor setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
        return this;
    }

    public String getLastError() {
        return lastError;
    }

    public JobDescriptor setLastError(String lastError) {
        this.lastError = lastError;
        return this;
    }

    public Instant getNextRunTime() {
        return nextRunTime;
    }

    public JobDescriptor setNextRunTime(Instant nextRunTime) {
        this.nextRunTime = nextRunTime;
        return this;
    }
}
