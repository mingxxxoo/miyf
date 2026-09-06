package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限元数据 + 运行时鉴权注解。
 * <ul>
 *   <li>启动扫描写入 {@code sys_permission}/{@code sys_perm_group}</li>
 *   <li>Spring Security 按 {@link #code()} 校验 Authority（若同方法另有 {@link RequirePermission} 则以后者为准）</li>
 * </ul>
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MiyfPermissions.class)
public @interface MiyfPermission {

    /**
     * 权限码，如 {@code kitchen:dish:list}。
     *
     * @return 权限码
     */
    String code();

    /**
     * 权限显示名。
     *
     * @return 名称
     */
    String name() default "";

    /**
     * 权限组编码，如 {@code kitchen_dish}。
     *
     * @return 组编码
     */
    String groupCode() default "";

    /**
     * 权限组显示名。
     *
     * @return 组名称
     */
    String groupName() default "";
}
