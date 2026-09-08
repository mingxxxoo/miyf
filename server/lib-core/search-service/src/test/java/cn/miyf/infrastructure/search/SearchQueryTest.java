package cn.miyf.infrastructure.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SearchQuery} 链式拼装与分页纠偏单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class SearchQueryTest {

    /**
     * 验证过滤、分页下限纠正与相关度排序可正确写入查询对象。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Test
    void filterAndPaging() {
        SearchQuery query = new SearchQuery()
                .setIndex("kitchen_dish")
                .setKeyword("番茄")
                .setFields(List.of("name^3", "subtitle"))
                .filter("status", "ON_SHELF")
                .setFrom(-1)
                .setSize(0)
                .sort(SearchSort.scoreDesc());
        assertEquals(0, query.getFrom());
        assertEquals(1, query.getSize());
        assertEquals("ON_SHELF", query.getFilters().get("status"));
        assertTrue(query.getSorts().stream().anyMatch(s -> "_score".equals(s.field())));
    }
}
