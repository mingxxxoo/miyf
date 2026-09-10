package cn.miyf.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明启动扫描可重建的角色定义。
 * <p>
 * 请标注在模块内 {@code @Component} 配置类上（可配合 {@link PopedomRoles} 重复），
 * 由 {@code PermissionBootstrap} 清空后重建 {@code sys_role}。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PopedomRoles.class)
public @interface PopedomRole {

    /**
     * 角色码（唯一）。
     */
    String code();

    /**
     * 角色名称。
     */
    String name();

    /**
     * 角色类型。
     */
    PopedomRoleType type();

    /**
     * 产品域；超管角色可为空。
     */
    String product() default "";

    /**
     * 描述。
     */
    String description() default "";
}
