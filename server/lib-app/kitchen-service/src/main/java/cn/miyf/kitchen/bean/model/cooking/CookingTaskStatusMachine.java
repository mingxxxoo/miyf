package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 做菜任务状态机：集中约束合法流转，避免任意状态互跳。
 * <p>
 * 领取只允许 PENDING → CLAIMED；开始制作允许 CLAIMED/PREPARING → COOKING；
 * 制作中可暂停/恢复；COOKING/PAUSED 可完成；完成与取消为终态。
 * 取消仅允许在开始制作前，提交人或领取厨师均可操作。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
public final class CookingTaskStatusMachine {

    private static final Map<CookingTaskStatus, Set<CookingTaskStatus>> TRANSITIONS =
            new EnumMap<>(CookingTaskStatus.class);

    static {
        TRANSITIONS.put(CookingTaskStatus.PENDING,
                EnumSet.of(CookingTaskStatus.CLAIMED, CookingTaskStatus.CANCELLED));
        TRANSITIONS.put(CookingTaskStatus.CLAIMED,
                EnumSet.of(CookingTaskStatus.PREPARING, CookingTaskStatus.PENDING,
                        CookingTaskStatus.CANCELLED));
        TRANSITIONS.put(CookingTaskStatus.PREPARING,
                EnumSet.of(CookingTaskStatus.COOKING, CookingTaskStatus.PENDING,
                        CookingTaskStatus.CANCELLED));
        TRANSITIONS.put(CookingTaskStatus.COOKING,
                EnumSet.of(CookingTaskStatus.PAUSED, CookingTaskStatus.COMPLETED));
        TRANSITIONS.put(CookingTaskStatus.PAUSED,
                EnumSet.of(CookingTaskStatus.COOKING, CookingTaskStatus.COMPLETED));
        TRANSITIONS.put(CookingTaskStatus.COMPLETED, EnumSet.noneOf(CookingTaskStatus.class));
        TRANSITIONS.put(CookingTaskStatus.CANCELLED, EnumSet.noneOf(CookingTaskStatus.class));
    }

    private CookingTaskStatusMachine() {
    }

    /**
     * 判断是否允许从 from 流转到 to。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 表示允许
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static boolean canTransit(CookingTaskStatus from, CookingTaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(CookingTaskStatus.class)).contains(to);
    }

    /**
     * 断言状态流转合法，否则抛出业务异常。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static void assertTransit(CookingTaskStatus from, CookingTaskStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS,
                    "任务无法从 " + from + " 流转到 " + to);
        }
    }

    /**
     * 是否允许领取：仅待领取。
     *
     * @param status 当前状态
     * @return true 表示可领取
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static boolean canClaim(CookingTaskStatus status) {
        return status == CookingTaskStatus.PENDING;
    }

    /**
     * 是否允许取消：开始制作前。
     *
     * @param status 当前状态
     * @return true 表示可取消
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static boolean canCancel(CookingTaskStatus status) {
        return status == CookingTaskStatus.PENDING
                || status == CookingTaskStatus.CLAIMED
                || status == CookingTaskStatus.PREPARING;
    }
}
