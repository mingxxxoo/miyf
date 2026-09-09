package cn.miyf.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link WebAppProperties} 前缀规范化与路径拼接单测。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
class WebAppPropertiesTest {

    @Test
    void normalizedApiPrefix_trimsSlashes() {
        WebAppProperties props = new WebAppProperties();
        props.setApiPrefix(" /api/ ");
        assertEquals("/api", props.normalizedApiPrefix());
    }

    @Test
    void normalizedApiPrefix_emptyMeansNoPrefix() {
        WebAppProperties props = new WebAppProperties();
        props.setApiPrefix("");
        assertEquals("", props.normalizedApiPrefix());
        assertEquals("/dishes", props.api("/dishes"));
    }

    @Test
    void api_joinsRelativePath() {
        WebAppProperties props = new WebAppProperties();
        props.setApiPrefix("/api");
        assertEquals("/api/dishes/**", props.api("/dishes/**"));
        assertEquals("/api/upload", props.api("upload"));
        assertEquals("/api/**", props.api("/**"));
    }
}
