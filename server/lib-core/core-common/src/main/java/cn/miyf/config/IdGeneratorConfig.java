package cn.miyf.config;

import cn.miyf.common.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花 ID 配置。
 *
 * @author XieMingJie
 * @since 2026-09-05 10:36
 */
@Configuration
public class IdGeneratorConfig {

    /**
     * 雪花生成器 Bean。
     *
     * @param workerId     机器位
     * @param datacenterId 机房位
     * @return 生成器
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${app.id.worker-id:1}") long workerId,
            @Value("${app.id.datacenter-id:1}") long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
}
