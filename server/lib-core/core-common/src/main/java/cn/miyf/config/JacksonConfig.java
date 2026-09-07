package cn.miyf.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.TimeZone;

/**
 * 提供 {@link ObjectMapper} Bean（Spring Boot 4 默认偏向 tools.jackson，业务侧仍使用 com.fasterxml）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:54
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局 ObjectMapper：JavaTime、非时间戳日期；Long 输出为字符串避免 JS 精度丢失。
     *
     * @return ObjectMapper
     * @history 1.00 2026-09-05 09:54 XieMingJie Created.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        SimpleModule longAsString = new SimpleModule();
        longAsString.addSerializer(Long.class, ToStringSerializer.instance);
        longAsString.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(longAsString);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return mapper;
    }
}
