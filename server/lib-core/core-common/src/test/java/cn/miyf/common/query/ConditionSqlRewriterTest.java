package cn.miyf.common.query;

import cn.miyf.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件 SQL 改写单测。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:50
 */
class ConditionSqlRewriterTest {

    @AfterEach
    void tearDown() {
        QueryConditionHolder.clear();
    }

    @Test
    void injectDefaultOrderWhenSortAbsent() {
        String sql = "SELECT * FROM dish d /* @conditionSql */ LIMIT 10 OFFSET 0";
        TestQo qo = new TestQo();
        QueryConditionHolder.Context ctx = QueryConditionHolder.Context.of(qo, "d.sort_order ASC, d.last_modify_time DESC", null);
        String rewritten = ConditionSqlRewriter.rewrite(sql, ctx);
        assertTrue(rewritten.contains("ORDER BY d.sort_order ASC, d.last_modify_time DESC"));
        assertTrue(!rewritten.contains("@conditionSql"));
    }

    @Test
    void injectQoSort() {
        String sql = "SELECT * FROM orders /* @conditionSql */ LIMIT #{limit} OFFSET #{offset}";
        TestQo qo = new TestQo();
        qo.setSort("create_time desc, order_no asc");
        QueryConditionHolder.Context ctx = QueryConditionHolder.Context.of(qo, "id ASC", null);
        String rewritten = ConditionSqlRewriter.rewrite(sql, ctx);
        assertTrue(rewritten.contains("ORDER BY create_time DESC, order_no ASC"));
    }

    @Test
    void rewriteAggregateOnly() {
        String sql = "SELECT id, rating FROM dish d /* @conditionSql */ LIMIT 10 OFFSET 0";
        TestQo qo = new TestQo();
        qo.setQueryRecord(false);
        qo.setAggregateJson("[{\"type\":\"avg\",\"field\":\"rating\"},{\"type\":\"count\",\"field\":\"*\"}]");
        QueryConditionHolder.Context ctx = QueryConditionHolder.Context.of(qo, "id ASC", null);
        String rewritten = ConditionSqlRewriter.rewrite(sql, ctx);
        assertTrue(rewritten.startsWith("SELECT "));
        assertTrue(rewritten.contains("AVG(rating) AS rating_avg"));
        assertTrue(rewritten.contains("COUNT(1) AS cnt"));
        assertTrue(rewritten.contains("_cond_agg"));
        assertTrue(!rewritten.contains("LIMIT"));
    }

    @Test
    void rejectUnsafeSortColumn() {
        TestQo qo = new TestQo();
        qo.setSort("id;drop table dish desc");
        assertThrows(BusinessException.class, qo::getSortList);
    }

    @Test
    void buildAggregateSelectList() {
        String select = ConditionSqlRewriter.buildAggregateSelectList(
                List.of(new ConditionAggregate("sum", "rating_count")), null);
        assertEquals("SUM(rating_count) AS rating_count_sum", select);
    }

    /**
     * 测试用 QO。
     */
    private static final class TestQo extends AbstractCondition {
    }
}
