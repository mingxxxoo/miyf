package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 做菜任务状态机单元测试。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 */
class CookingTaskStatusMachineTest {

    @Test
    void allowsHappyPath() {
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PENDING, CookingTaskStatus.CLAIMED));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.CLAIMED, CookingTaskStatus.PREPARING));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PREPARING, CookingTaskStatus.COOKING));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.COOKING, CookingTaskStatus.PAUSED));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PAUSED, CookingTaskStatus.COOKING));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.COOKING, CookingTaskStatus.COMPLETED));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PAUSED, CookingTaskStatus.COMPLETED));
    }

    @Test
    void allowsReleaseBeforeCooking() {
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.CLAIMED, CookingTaskStatus.PENDING));
        assertTrue(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PREPARING, CookingTaskStatus.PENDING));
    }

    @Test
    void allowsCancelOnlyBeforeCooking() {
        assertTrue(CookingTaskStatusMachine.canCancel(CookingTaskStatus.PENDING));
        assertTrue(CookingTaskStatusMachine.canCancel(CookingTaskStatus.CLAIMED));
        assertTrue(CookingTaskStatusMachine.canCancel(CookingTaskStatus.PREPARING));
        assertFalse(CookingTaskStatusMachine.canCancel(CookingTaskStatus.COOKING));
        assertFalse(CookingTaskStatusMachine.canCancel(CookingTaskStatus.PAUSED));
        assertFalse(CookingTaskStatusMachine.canCancel(CookingTaskStatus.COMPLETED));
    }

    @Test
    void rejectsIllegalTransitions() {
        assertFalse(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.PENDING, CookingTaskStatus.COOKING));
        assertFalse(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.COMPLETED, CookingTaskStatus.COOKING));
        assertFalse(CookingTaskStatusMachine.canTransit(
                CookingTaskStatus.CANCELLED, CookingTaskStatus.PENDING));
        assertThrows(BusinessException.class, () ->
                CookingTaskStatusMachine.assertTransit(
                        CookingTaskStatus.COMPLETED, CookingTaskStatus.COOKING));
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (CookingTaskStatus to : CookingTaskStatus.values()) {
            assertFalse(CookingTaskStatusMachine.canTransit(CookingTaskStatus.COMPLETED, to));
            assertFalse(CookingTaskStatusMachine.canTransit(CookingTaskStatus.CANCELLED, to));
        }
    }

    @Test
    void onlyPendingCanBeClaimed() {
        assertTrue(CookingTaskStatusMachine.canClaim(CookingTaskStatus.PENDING));
        assertFalse(CookingTaskStatusMachine.canClaim(CookingTaskStatus.CLAIMED));
        assertFalse(CookingTaskStatusMachine.canClaim(CookingTaskStatus.COOKING));
    }
}
