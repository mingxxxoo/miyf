package cn.miyf.kitchen.bean.model.cooking;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 做菜任务步骤状态机。
 * <p>
 * PENDING → RUNNING → PAUSED ⇄ RUNNING → DONE；DONE 可撤销回 PENDING。
 * 撤销只允许针对最后一个已完成步骤，避免跳过中间步骤造成状态错乱。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
public final class CookingTaskStepStatusMachine {

    private static final Map<CookingTaskStepStatus, Set<CookingTaskStepStatus>> TRANSITIONS =
            new EnumMap<>(CookingTaskStepStatus.class);

    static {
        TRANSITIONS.put(CookingTaskStepStatus.PENDING,
                EnumSet.of(CookingTaskStepStatus.RUNNING, CookingTaskStepStatus.DONE));
        TRANSITIONS.put(CookingTaskStepStatus.RUNNING,
                EnumSet.of(CookingTaskStepStatus.PAUSED, CookingTaskStepStatus.DONE));
        TRANSITIONS.put(CookingTaskStepStatus.PAUSED,
                EnumSet.of(CookingTaskStepStatus.RUNNING, CookingTaskStepStatus.DONE));
        TRANSITIONS.put(CookingTaskStepStatus.DONE,
                EnumSet.of(CookingTaskStepStatus.PENDING));
    }

    private CookingTaskStepStatusMachine() {
    }

    /**
     * 判断步骤是否允许流转。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 表示允许
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canTransit(CookingTaskStepStatus from, CookingTaskStepStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(CookingTaskStepStatus.class)).contains(to);
    }

    /**
     * 是否可开始：仅未开始。
     *
     * @param status 当前状态
     * @return true 表示可开始
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canStart(CookingTaskStepStatus status) {
        return status == CookingTaskStepStatus.PENDING;
    }

    /**
     * 是否可暂停：仅进行中。
     *
     * @param status 当前状态
     * @return true 表示可暂停
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canPause(CookingTaskStepStatus status) {
        return status == CookingTaskStepStatus.RUNNING;
    }

    /**
     * 是否可继续：仅已暂停。
     *
     * @param status 当前状态
     * @return true 表示可继续
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canResume(CookingTaskStepStatus status) {
        return status == CookingTaskStepStatus.PAUSED;
    }

    /**
     * 是否可完成：未完成的步骤均可完成。
     *
     * @param status 当前状态
     * @return true 表示可完成
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canComplete(CookingTaskStepStatus status) {
        return status == CookingTaskStepStatus.PENDING
                || status == CookingTaskStepStatus.RUNNING
                || status == CookingTaskStepStatus.PAUSED;
    }

    /**
     * 是否可撤销：仅已完成。
     *
     * @param status 当前状态
     * @return true 表示可撤销
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static boolean canUndo(CookingTaskStepStatus status) {
        return status == CookingTaskStepStatus.DONE;
    }
}
