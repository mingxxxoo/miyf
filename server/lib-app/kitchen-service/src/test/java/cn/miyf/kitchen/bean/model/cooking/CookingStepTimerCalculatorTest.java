package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.kitchen.bean.entity.CookingTaskStepEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 做菜步骤计时计算器单元测试。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 */
class CookingStepTimerCalculatorTest {

    private static final Instant BASE = Instant.parse("2026-10-31T08:00:00Z");

    @Test
    void runningCountdownCountsDownByServerTime() {
        CookingTaskStepEntity step = step("RUNNING", "countdown", 300);
        step.setStartedTime(BASE);

        assertEquals(300, CookingStepTimerCalculator.remainingSeconds(step, BASE));
        assertEquals(120, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(180)));
        assertEquals(0, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(900)));
        assertEquals(180, CookingStepTimerCalculator.elapsedSeconds(step, BASE.plusSeconds(180)));
    }

    @Test
    void pausedCountdownFreezesAtPauseTime() {
        CookingTaskStepEntity step = step("PAUSED", "countdown", 600);
        step.setStartedTime(BASE);
        step.setPausedTime(BASE.plusSeconds(100));

        assertEquals(500, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(100)));
        assertEquals(500, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(900)));
    }

    @Test
    void resumedStepExcludesPausedSeconds() {
        CookingTaskStepEntity step = step("RUNNING", "countdown", 600);
        step.setStartedTime(BASE);
        step.setTotalPausedSeconds(90);

        // 已过 300 秒，其中 90 秒在暂停，净用时 210 秒
        assertEquals(390, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(300)));
        assertEquals(210, CookingStepTimerCalculator.elapsedSeconds(step, BASE.plusSeconds(300)));
    }

    @Test
    void countupStepReportsElapsedWithoutRemaining() {
        CookingTaskStepEntity step = step("RUNNING", "countup", null);
        step.setStartedTime(BASE);

        assertEquals(75, CookingStepTimerCalculator.elapsedSeconds(step, BASE.plusSeconds(75)));
        assertNull(CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(75)));
    }

    @Test
    void doneCountdownIsZero() {
        CookingTaskStepEntity step = step("DONE", "countdown", 300);
        step.setStartedTime(BASE);
        step.setCompletedTime(BASE.plusSeconds(240));

        assertEquals(0, CookingStepTimerCalculator.remainingSeconds(step, BASE.plusSeconds(999)));
        assertEquals(240, CookingStepTimerCalculator.elapsedSeconds(step, BASE.plusSeconds(999)));
    }

    @Test
    void pendingAndNoneTimersReturnNulls() {
        CookingTaskStepEntity pending = step("PENDING", "countdown", 120);
        assertEquals(120, CookingStepTimerCalculator.remainingSeconds(pending, BASE));
        assertNull(CookingStepTimerCalculator.elapsedSeconds(pending, BASE));

        CookingTaskStepEntity none = step("RUNNING", "none", 120);
        none.setStartedTime(BASE);
        assertNull(CookingStepTimerCalculator.elapsedSeconds(none, BASE.plusSeconds(60)));
        assertNull(CookingStepTimerCalculator.remainingSeconds(none, BASE.plusSeconds(60)));
    }

    private CookingTaskStepEntity step(String status, String timerType, Integer durationSeconds) {
        CookingTaskStepEntity step = new CookingTaskStepEntity()
                .setStatus(status)
                .setTimerType(timerType)
                .setDurationSeconds(durationSeconds)
                .setTotalPausedSeconds(0);
        step.setStepOrder(1);
        return step;
    }
}
