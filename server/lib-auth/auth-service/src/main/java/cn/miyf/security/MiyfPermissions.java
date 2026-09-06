package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link MiyfPermission} 可重复容器。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MiyfPermissions {

    /**
     * 权限元数据列表。
     *
     * @return 注解数组
     */
    MiyfPermission[] value();
}
