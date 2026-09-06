package cn.miyf.common.id;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 标识生成：委托 {@link SnowflakeIdGenerator}。
 *
 * @author XieMingJie
 * @since 2026-09-05 10:36
 */
@Component
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 构造。
     *
     * @param snowflakeIdGenerator 雪花生成器
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    public SnowflakeIdentifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    @Override
    public Number nextId(Object entity) {
        return snowflakeIdGenerator.nextId();
    }
}
