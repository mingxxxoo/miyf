package cn.miyf.kitchen.bean.model.order;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.model.OrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 预约单状态机：约束合法流转，禁止任意状态互跳。
 * <p>
 * 正常链路：PENDING → CONFIRMED → PREPARING → READY → COMPLETED；
 * 用户取消仅允许 PENDING → CANCELLED。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public final class OrderStatusMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        // 待确认：可确认或取消
        TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING));
        TRANSITIONS.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY));
        TRANSITIONS.put(OrderStatus.READY, EnumSet.of(OrderStatus.COMPLETED));
        // 终态不可再流转
        TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusMachine() {
    }

    /**
     * 判断是否允许从 from 流转到 to。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 表示允许
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        return allowed.contains(to);
    }

    /**
     * 断言状态流转合法，否则抛出业务异常。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public static void assertTransit(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS,
                    "无法从 " + from + " 流转到 " + to);
        }
    }

    /**
     * 用户是否可取消：仅待确认允许。
     *
     * @param current 当前状态
     * @return true 表示可取消
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    public static boolean canUserCancel(OrderStatus current) {
        return current == OrderStatus.PENDING;
    }
}
