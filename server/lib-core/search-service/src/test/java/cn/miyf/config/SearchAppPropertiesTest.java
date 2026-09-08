package cn.miyf.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link SearchAppProperties} 索引名解析单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class SearchAppPropertiesTest {

    /**
     * 验证前缀拼接与已带前缀时不重复拼接。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Test
    void resolveIndexWithPrefix() {
        SearchAppProperties properties = new SearchAppProperties();
        properties.setIndexPrefix("miyf");
        assertEquals("miyf_kitchen_dish", properties.resolveIndex("kitchen_dish"));
        assertEquals("miyf_kitchen_dish", properties.resolveIndex("miyf_kitchen_dish"));
    }

    /**
     * 验证空白逻辑名抛出非法参数异常。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Test
    void rejectBlankLogicalName() {
        SearchAppProperties properties = new SearchAppProperties();
        assertThrows(IllegalArgumentException.class, () -> properties.resolveIndex(" "));
    }
}
