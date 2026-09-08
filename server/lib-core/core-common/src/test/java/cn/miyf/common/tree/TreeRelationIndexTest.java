package cn.miyf.common.tree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 父子关系索引单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class TreeRelationIndexTest {

    @Test
    void selfAndDescendantsUnlimited() {
        record Unit(Long id, Long parentId) {
        }
        List<Unit> all = List.of(
                new Unit(1L, null),
                new Unit(2L, 1L),
                new Unit(3L, 2L),
                new Unit(4L, 3L),
                new Unit(9L, null)
        );
        TreeRelationIndex<Long> index = TreeRelationIndex.of(all, Unit::id, Unit::parentId);
        assertEquals(Set.of(1L, 2L, 3L, 4L), index.selfAndDescendants(1L));
        assertEquals(Set.of(3L, 4L), index.selfAndDescendants(3L));
        assertEquals(List.of(2L), index.childrenOf(1L));
        assertTrue(index.selfAndDescendants(null).isEmpty());
    }
}
