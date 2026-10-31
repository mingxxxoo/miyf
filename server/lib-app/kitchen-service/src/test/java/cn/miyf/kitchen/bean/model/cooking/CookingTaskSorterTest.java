package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.kitchen.bean.entity.CookingTaskEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 做菜任务排序单元测试。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 */
class CookingTaskSorterTest {

    private static final Instant BASE = Instant.parse("2026-10-31T08:00:00Z");

    @Test
    void sortsActiveThenClaimedThenPending() {
        CookingTaskEntity pending = task(1L, "PENDING", 9, BASE.plusSeconds(60), BASE);
        CookingTaskEntity claimed = task(2L, "CLAIMED", 0, null, BASE);
        CookingTaskEntity cooking = task(3L, "COOKING", 0, null, BASE);

        List<CookingTaskEntity> sorted = CookingTaskSorter.sort(List.of(pending, claimed, cooking));

        assertEquals(3L, sorted.get(0).getId());
        assertEquals(2L, sorted.get(1).getId());
        assertEquals(1L, sorted.get(2).getId());
    }

    @Test
    void earlierScheduledStartWinsBeforePriority() {
        CookingTaskEntity later = task(1L, "PENDING", 9, BASE.plusSeconds(600), BASE);
        CookingTaskEntity earlier = task(2L, "PENDING", 0, BASE.plusSeconds(60), BASE);
        CookingTaskEntity unscheduled = task(3L, "PENDING", 9, null, BASE);

        List<CookingTaskEntity> sorted =
                CookingTaskSorter.sort(List.of(later, earlier, unscheduled));

        assertEquals(2L, sorted.get(0).getId());
        assertEquals(1L, sorted.get(1).getId());
        assertEquals(3L, sorted.get(2).getId());
    }

    @Test
    void higherPriorityWinsWhenScheduleEqual() {
        CookingTaskEntity low = task(1L, "PENDING", 1, BASE, BASE);
        CookingTaskEntity high = task(2L, "PENDING", 8, BASE, BASE);

        List<CookingTaskEntity> sorted = CookingTaskSorter.sort(List.of(low, high));

        assertEquals(2L, sorted.get(0).getId());
        assertEquals(1L, sorted.get(1).getId());
    }

    @Test
    void earlierCreateTimeWinsWhenOtherKeysEqual() {
        CookingTaskEntity later = task(1L, "PENDING", 0, BASE, BASE.plusSeconds(120));
        CookingTaskEntity earlier = task(2L, "PENDING", 0, BASE, BASE.plusSeconds(30));

        List<CookingTaskEntity> sorted = CookingTaskSorter.sort(List.of(later, earlier));

        assertEquals(2L, sorted.get(0).getId());
        assertEquals(1L, sorted.get(1).getId());
    }

    @Test
    void doesNotMutateInputAndHandlesEmpty() {
        List<CookingTaskEntity> input = new ArrayList<>();
        input.add(task(1L, "PENDING", 0, null, BASE));
        input.add(task(2L, "COOKING", 0, null, BASE));

        CookingTaskSorter.sort(input);

        assertEquals(1L, input.get(0).getId());
        assertEquals(0, CookingTaskSorter.sort(null).size());
        assertNull(CookingTaskSorter.sort(List.of()).stream().findFirst().orElse(null));
    }

    private CookingTaskEntity task(Long id,
                                   String status,
                                   Integer priority,
                                   Instant scheduledStartTime,
                                   Instant createTime) {
        CookingTaskEntity task = new CookingTaskEntity()
                .setStatus(status)
                .setPriority(priority)
                .setScheduledStartTime(scheduledStartTime);
        task.setId(id);
        task.setCreateTime(createTime);
        return task;
    }
}
