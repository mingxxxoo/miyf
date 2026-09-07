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
 *   <li>显示名优先 {@link #name()}，否则取同方法 {@code @Operation#summary()}，再否则用 {@link #code()}</li>
 *   <li>权限组取 Controller 上的 {@link PopedomGroup}（推荐用模块组合注解，如 {@link IamAdminPopedom}）</li>
 *   <li>Spring Security 按 {@link #code()} 校验 Authority；同方法若另有 {@link RequirePermission}（多码 OR）则以后者为准</li>
 * </ul>
 * <p>
 * 常规写法：类上 {@code @XxxPopedom}，方法上 {@code @Operation} + {@code @MiyfPermission(code = "...")} 即可。
 * 仅当一个接口允许「多个权限码之一」时，再额外加 {@link RequirePermission}。
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
     * 权限显示名；留空则启动扫描时使用 {@code @Operation(summary)}。
     *
     * @return 名称
     * @deprecated 优先用 {@code @Operation(summary)}，无需再填
     */
    @Deprecated
    String name() default "";

    /**
     * 已废弃：组编码由类上 {@link PopedomGroup} 决定。
     *
     * @return 组编码
     * @deprecated 使用 {@link PopedomGroup#value()}
     */
    @Deprecated
    String groupCode() default "";

    /**
     * 已废弃：组名称由类上 {@link PopedomGroup} 决定。
     *
     * @return 组名称
     * @deprecated 使用 {@link PopedomGroup#name()}
     */
    @Deprecated
    String groupName() default "";
}
