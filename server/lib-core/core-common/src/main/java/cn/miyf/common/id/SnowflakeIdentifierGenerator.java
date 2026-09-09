package cn.miyf.common.id;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 标识生成：委托 {@link SnowflakeIdGenerator}。
 *
 * @author XieMingJie
 * @since 2026-09-05 10:36
 */
@Component
@RequiredArgsConstructor
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    
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
