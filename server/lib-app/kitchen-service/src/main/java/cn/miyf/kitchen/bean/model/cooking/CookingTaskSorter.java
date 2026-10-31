package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.kitchen.bean.entity.CookingTaskEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 做菜任务排序：独立、可替换的排序函数。
 * <p>
 * 排序优先级（从前到后）：
 * <ol>
 *     <li>已开始但未完成的任务优先（COOKING / PAUSED）；</li>
 *     <li>已被领取的任务优先于待领取（CLAIMED / PREPARING 优先于 PENDING）；</li>
 *     <li>计划开始时间更早的优先，未设置计划时间的排最后；</li>
 *     <li>优先级数值更大的优先；</li>
 *     <li>创建时间更早的优先。</li>
 * </ol>
 * 排序逻辑集中在此类，后续如要调整顺序只需替换 {@link #comparator()}。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
public final class CookingTaskSorter {

    private static final int RANK_ACTIVE = 0;
    private static final int RANK_CLAIMED = 1;
    private static final int RANK_PENDING = 2;
    private static final int RANK_OTHER = 3;

    private static final Comparator<CookingTaskEntity> COMPARATOR =
            Comparator.comparingInt(CookingTaskSorter::rank)
                    .thenComparing(CookingTaskSorter::scheduledStartTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Comparator.comparingInt(CookingTaskSorter::priority).reversed())
                    .thenComparing(CookingTaskSorter::createTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CookingTaskSorter::stableId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private CookingTaskSorter() {
    }

    /**
     * 返回排序比较器；外部如要自定义排序，可直接替换此方法实现。
     *
     * @return 任务比较器
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static Comparator<CookingTaskEntity> comparator() {
        return COMPARATOR;
    }

    /**
     * 对任务集合排序，返回新列表，不修改入参。
     *
     * @param tasks 任务集合
     * @return 排序后的列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static List<CookingTaskEntity> sort(Collection<CookingTaskEntity> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        List<CookingTaskEntity> list = new ArrayList<>(tasks);
        list.sort(COMPARATOR);
        return list;
    }

    /**
     * 是否已开始但未完成；兼容已写入开始时间但状态滞后的历史数据。
     *
     * @param task 任务
     * @return true 表示进行中
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    static boolean active(CookingTaskEntity task) {
        CookingTaskStatus status = safeStatus(task);
        if (status == CookingTaskStatus.COOKING || status == CookingTaskStatus.PAUSED) {
            return true;
        }
        return status != null && !status.terminal()
                && status != CookingTaskStatus.PENDING
                && task.getStartedTime() != null;
    }

    private static int rank(CookingTaskEntity task) {
        CookingTaskStatus status = safeStatus(task);
        if (status == null) {
            return RANK_OTHER;
        }
        if (active(task)) {
            return RANK_ACTIVE;
        }
        if (status == CookingTaskStatus.CLAIMED || status == CookingTaskStatus.PREPARING) {
            return RANK_CLAIMED;
        }
        if (status == CookingTaskStatus.PENDING) {
            return RANK_PENDING;
        }
        return RANK_OTHER;
    }

    private static CookingTaskStatus safeStatus(CookingTaskEntity task) {
        if (task == null || task.getStatus() == null) {
            return null;
        }
        try {
            return CookingTaskStatus.valueOf(task.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant scheduledStartTime(CookingTaskEntity task) {
        return task == null ? null : task.getScheduledStartTime();
    }

    private static int priority(CookingTaskEntity task) {
        if (task == null || task.getPriority() == null) {
            return 0;
        }
        return task.getPriority();
    }

    private static Instant createTime(CookingTaskEntity task) {
        return task == null ? null : task.getCreateTime();
    }

    private static Long stableId(CookingTaskEntity task) {
        return task == null ? null : task.getId();
    }
}
