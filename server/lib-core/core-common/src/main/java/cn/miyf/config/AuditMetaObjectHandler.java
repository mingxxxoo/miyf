package cn.miyf.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 实体审计字段自动填充：新增写 createTime/lastModifyTime，修改强制刷新 lastModifyTime。
 * <p>
 * 依赖实体字段上的 {@code @TableField(fill = ...)}；手写 XML INSERT/UPDATE 由
 * {@link cn.miyf.interceptor.AuditSqlInterceptor} 在 SQL 层注入审计时间列。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入填充创建时间与最后修改时间（仅当字段为 null 时写入 createTime）。
     *
     * @param metaObject 元对象
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        strictInsertFill(metaObject, "createTime", Instant.class, now);
        // 插入时始终对齐最后修改时间，避免与 createTime 不一致
        setFieldValByName("lastModifyTime", now, metaObject);
    }

    /**
     * 更新时强制刷新最后修改时间（覆盖调用方已设值）。
     *
     * @param metaObject 元对象
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("lastModifyTime", Instant.now(), metaObject);
    }
}
