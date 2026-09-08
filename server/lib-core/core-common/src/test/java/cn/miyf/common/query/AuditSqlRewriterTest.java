package cn.miyf.common.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计 SQL 改写单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class AuditSqlRewriterTest {

    @Test
    void injectLastModifyTimeAfterSet() {
        String sql = """
                UPDATE dish
                SET stock = stock - #{qty}
                WHERE id = #{id}
                """;
        String rewritten = AuditSqlRewriter.rewrite(sql);
        assertEquals("""
                UPDATE dish
                SET last_modify_time = NOW(), stock = stock - #{qty}
                WHERE id = #{id}
                """, rewritten);
    }

    @Test
    void skipUpdateWhenAlreadyAssigned() {
        String sql = """
                UPDATE kitchen_order
                SET status = #{status},
                    last_modify_time = NOW()
                WHERE id = #{id}
                """;
        assertSame(sql, AuditSqlRewriter.rewrite(sql));
    }

    @Test
    void injectInsertAuditColumnsForValues() {
        String sql = "INSERT INTO dish (id, name) VALUES (#{id}, #{name})";
        String rewritten = AuditSqlRewriter.rewrite(sql);
        assertEquals(
                "INSERT INTO dish (id, name, create_time, last_modify_time) VALUES (#{id}, #{name}, NOW(), NOW())",
                rewritten);
    }

    @Test
    void injectInsertAuditColumnsForMultiValues() {
        String sql = "INSERT INTO dish (id, name) VALUES (#{id1}, #{name1}), (#{id2}, #{name2})";
        String rewritten = AuditSqlRewriter.rewrite(sql);
        assertEquals(
                "INSERT INTO dish (id, name, create_time, last_modify_time) "
                        + "VALUES (#{id1}, #{name1}, NOW(), NOW()), (#{id2}, #{name2}, NOW(), NOW())",
                rewritten);
    }

    @Test
    void injectOnlyMissingInsertColumn() {
        String sql = "INSERT INTO dish (id, name, create_time) VALUES (#{id}, #{name}, NOW())";
        String rewritten = AuditSqlRewriter.rewrite(sql);
        assertEquals(
                "INSERT INTO dish (id, name, create_time, last_modify_time) VALUES (#{id}, #{name}, NOW(), NOW())",
                rewritten);
    }

    @Test
    void skipInsertWhenAuditColumnsPresent() {
        String sql = "INSERT INTO dish (id, create_time, last_modify_time) VALUES (#{id}, #{ct}, #{lmt})";
        assertSame(sql, AuditSqlRewriter.rewrite(sql));
    }

    @Test
    void injectInsertSelectAuditColumns() {
        String sql = "INSERT INTO dish (id, name) SELECT id, name FROM tmp";
        String rewritten = AuditSqlRewriter.rewrite(sql);
        assertTrue(rewritten.contains("create_time, last_modify_time"));
        assertEquals(
                "INSERT INTO dish (id, name, create_time, last_modify_time) SELECT id, name, NOW(), NOW() FROM tmp",
                rewritten);
    }
}
