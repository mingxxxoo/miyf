package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.kitchen.bean.entity.CookingTaskStepEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * 做菜步骤计时计算器。
 * <p>
 * 计时以服务端时间为准：进行中按当前时间计算，暂停时以暂停时间冻结，
 * 已完成以完成时间收口；累计暂停时长始终从净用时中扣除。
 * 前端刷新、退出重进后均可据此恢复倒计时，无需依赖本地计时。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
public final class CookingStepTimerCalculator {

    private static final String COUNTDOWN = "countdown";
    private static final String NONE = "none";

    private CookingStepTimerCalculator() {
    }

    /**
     * 计算步骤净用时（秒），未开始或不计时返回 null。
     *
     * @param step 步骤实体
     * @param now  服务端当前时间
     * @return 净用时秒数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static Integer elapsedSeconds(CookingTaskStepEntity step, Instant now) {
        if (step == null || step.getStartedTime() == null || step.getStatus() == null) {
            return null;
        }
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.PENDING) {
            return null;
        }
        if (NONE.equals(normalizeTimerType(step.getTimerType()))) {
            return null;
        }
        Instant end = resolveEnd(step, status, now);
        if (end == null || end.isBefore(step.getStartedTime())) {
            return 0;
        }
        long paused = step.getTotalPausedSeconds() == null ? 0 : step.getTotalPausedSeconds();
        long elapsed = Duration.between(step.getStartedTime(), end).getSeconds() - paused;
        return (int) Math.max(0, elapsed);
    }

    /**
     * 计算倒计时剩余秒数；非倒计时返回 null。
     *
     * @param step 步骤实体
     * @param now  服务端当前时间
     * @return 剩余秒数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static Integer remainingSeconds(CookingTaskStepEntity step, Instant now) {
        if (step == null || step.getDurationSeconds() == null) {
            return null;
        }
        if (!COUNTDOWN.equals(normalizeTimerType(step.getTimerType()))) {
            return null;
        }
        if (step.getStatus() == null) {
            return step.getDurationSeconds();
        }
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.PENDING) {
            return step.getDurationSeconds();
        }
        if (status == CookingTaskStepStatus.DONE) {
            return 0;
        }
        Integer elapsed = elapsedSeconds(step, now);
        if (elapsed == null) {
            return step.getDurationSeconds();
        }
        return Math.max(0, step.getDurationSeconds() - elapsed);
    }

    private static Instant resolveEnd(CookingTaskStepEntity step,
                                      CookingTaskStepStatus status,
                                      Instant now) {
        return switch (status) {
            case RUNNING -> now;
            case PAUSED -> step.getPausedTime() == null ? now : step.getPausedTime();
            case DONE -> step.getCompletedTime() == null ? now : step.getCompletedTime();
            case PENDING -> null;
        };
    }

    private static String normalizeTimerType(String timerType) {
        if (timerType == null) {
            return NONE;
        }
        return timerType.trim().toLowerCase(Locale.ROOT);
    }
}
