package cn.miyf.infrastructure.search;

import cn.miyf.common.query.AbstractCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Condition → SearchQuery 语义对齐单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class SearchQueriesTest {

    @Test
    void pagingAndKeywordAlignWithCondition() {
        TestCondition qo = new TestCondition();
        qo.setPage(2);
        qo.setRows(20);
        qo.setKeyword(" 番茄 ");
        qo.setSort("sort_order asc, create_time desc");

        SearchFieldMapping mapping = SearchFieldMapping.create()
                .map("sort_order", "sortOrder")
                .map("create_time", "createTimeEpochMs");

        SearchQuery query = SearchQueries.index("kitchen_dish")
                .condition(qo)
                .keywordFields("name^3", "subtitle")
                .filter("categoryId", 9L)
                .fieldMapping(mapping)
                .defaultOrder("is_recommend DESC, sort_order ASC")
                .preferScoreWhenKeyword(true)
                .build();

        assertEquals(20, query.getFrom());
        assertEquals(20, query.getSize());
        assertEquals("番茄", query.getKeyword());
        assertEquals(9L, query.getFilters().get("categoryId"));
        assertEquals(List.of("name^3", "subtitle"), query.getFields());
        assertTrue(query.getSorts().stream().anyMatch(s -> "_score".equals(s.field())));
        assertEquals("sortOrder", query.getSorts().get(1).field());
        assertTrue(query.getSorts().get(1).asc());
        assertEquals("createTimeEpochMs", query.getSorts().get(2).field());
    }

    @Test
    void defaultOrderWhenNoRequestSort() {
        TestCondition qo = new TestCondition();
        qo.setPage(1);
        qo.setRows(10);

        SearchQuery query = SearchQueries.index("kitchen_dish")
                .condition(qo)
                .fieldMapping(SearchFieldMapping.create().map("d.is_recommend", "recommend"))
                .defaultOrder("d.is_recommend DESC, d.sort_order ASC")
                .preferScoreWhenKeyword(false)
                .build();

        assertEquals(0, query.getFrom());
        assertEquals("recommend", query.getSorts().get(0).field());
        assertEquals(false, query.getSorts().get(0).asc());
        assertEquals("sort_order", query.getSorts().get(1).field());
    }

    private static final class TestCondition extends AbstractCondition {
    }
}
