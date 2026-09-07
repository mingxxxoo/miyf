package cn.miyf.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * HTTP JSON（Spring Boot 4 / tools.jackson）定制：Long 输出为字符串，避免前端 JS 精度丢失。
 * <p>
 * 入参侧的雪花 ID 由各 SaveDto 使用字符串字段承接，再在服务层解析为 Long。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Configuration
public class HttpJacksonConfig {

    /**
     * 注册全局 Long → String 序列化（作用于 MVC 响应；入参 ID 由 DTO 字符串字段承接）。
     *
     * @return JsonMapperBuilderCustomizer
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Bean
    public JsonMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("miyfLongAsString");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}
