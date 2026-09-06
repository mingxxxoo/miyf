package cn.miyf.kitchen.bean.model.order;

import cn.miyf.common.BusinessException;
import cn.miyf.kitchen.bean.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预约状态机单测。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
class OrderStatusMachineTest {

    @Test
    void shouldAllowLegalTransitionsOnly() {
        assertTrue(OrderStatusMachine.canTransit(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertTrue(OrderStatusMachine.canTransit(OrderStatus.PENDING, OrderStatus.CANCELLED));
        assertTrue(OrderStatusMachine.canUserCancel(OrderStatus.PENDING));
        assertFalse(OrderStatusMachine.canUserCancel(OrderStatus.CONFIRMED));
        assertFalse(OrderStatusMachine.canTransit(OrderStatus.COMPLETED, OrderStatus.PENDING));
        assertThrows(BusinessException.class,
                () -> OrderStatusMachine.assertTransit(OrderStatus.READY, OrderStatus.PENDING));
    }
}