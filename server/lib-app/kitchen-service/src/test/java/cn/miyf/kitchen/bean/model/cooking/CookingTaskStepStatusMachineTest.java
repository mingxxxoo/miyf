package cn.miyf.kitchen.bean.model.cooking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 做菜步骤状态机单元测试。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 */
class CookingTaskStepStatusMachineTest {

    @Test
    void pendingCanStartOrCompleteDirectly() {
        assertTrue(CookingTaskStepStatusMachine.canStart(CookingTaskStepStatus.PENDING));
        assertTrue(CookingTaskStepStatusMachine.canComplete(CookingTaskStepStatus.PENDING));
        assertFalse(CookingTaskStepStatusMachine.canPause(CookingTaskStepStatus.PENDING));
        assertFalse(CookingTaskStepStatusMachine.canUndo(CookingTaskStepStatus.PENDING));
    }

    @Test
    void runningCanPauseAndComplete() {
        assertTrue(CookingTaskStepStatusMachine.canPause(CookingTaskStepStatus.RUNNING));
        assertTrue(CookingTaskStepStatusMachine.canComplete(CookingTaskStepStatus.RUNNING));
        assertFalse(CookingTaskStepStatusMachine.canStart(CookingTaskStepStatus.RUNNING));
    }

    @Test
    void pausedCanResumeAndComplete() {
        assertTrue(CookingTaskStepStatusMachine.canResume(CookingTaskStepStatus.PAUSED));
        assertTrue(CookingTaskStepStatusMachine.canComplete(CookingTaskStepStatus.PAUSED));
        assertFalse(CookingTaskStepStatusMachine.canPause(CookingTaskStepStatus.PAUSED));
    }

    @Test
    void doneCanOnlyBeUndone() {
        assertTrue(CookingTaskStepStatusMachine.canUndo(CookingTaskStepStatus.DONE));
        assertFalse(CookingTaskStepStatusMachine.canStart(CookingTaskStepStatus.DONE));
        assertFalse(CookingTaskStepStatusMachine.canComplete(CookingTaskStepStatus.DONE));
        assertTrue(CookingTaskStepStatusMachine.canTransit(
                CookingTaskStepStatus.DONE, CookingTaskStepStatus.PENDING));
    }

    @Test
    void illegalTransitionsRejected() {
        assertFalse(CookingTaskStepStatusMachine.canTransit(
                CookingTaskStepStatus.PENDING, CookingTaskStepStatus.PAUSED));
        assertTrue(CookingTaskStepStatusMachine.canTransit(
                CookingTaskStepStatus.PENDING, CookingTaskStepStatus.DONE));
        assertFalse(CookingTaskStepStatusMachine.canTransit(
                CookingTaskStepStatus.RUNNING, CookingTaskStepStatus.PENDING));
        assertFalse(CookingTaskStepStatusMachine.canTransit(null, CookingTaskStepStatus.RUNNING));
    }

    @Test
    void parsesStatusText() {
        assertTrue(CookingTaskStepStatus.from("running") == CookingTaskStepStatus.RUNNING);
        assertTrue(CookingTaskStepStatus.from("Paused") == CookingTaskStepStatus.PAUSED);
        assertFalse(CookingTaskStepStatus.PENDING.finished());
        assertTrue(CookingTaskStepStatus.DONE.finished());
    }
}
