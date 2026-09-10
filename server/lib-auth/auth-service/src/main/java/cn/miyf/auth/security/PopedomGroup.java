package cn.miyf.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器级权限组声明。启动扫描写入权限组，方法级权限挂到本组。
 * <p>
 * {@code value} 为 8 位权限组编码。请勿在业务代码中手写数字，优先使用模块组合注解。
 * 展示名优先 {@link #name()}；为空时按 {@code scope.label + "-" + service} 生成（如「个人-厨房服务」）。
 * 鉴权域判断必须使用 {@link #scope()}，禁止用名称字符串比对。
 * <p>
 * {@link #roles()} 声明绑定到本组的角色码；启动重建时写入 {@code sys_role_perm_group}。
 * 权限树层级：{@code 个人|单位|超管 → 接口业务 → 接口名称}。
 *
 * @author XieMingJie
 * @see docs/AUTH_PERMISSION_PLAN.md
 * @since 2026-09-06
 * @history 1.00 2026-09-06 XieMingJie Created.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PopedomGroup {

    /**
     * 8 位权限组编码。
     */
    String value();

    /**
     * 权限域：个人 / 单位 / 超管。
     */
    PopedomScope scope();

    /**
     * 服务展示名（如「厨房服务」「健康服务」），与 scope 拼成组名。
     */
    String service();

    /**
     * 完整展示名；留空则 {@code scope.label + "-" + service}。
     */
    String name() default "";

    /**
     * 产品/业务域编码（如 kitchen、health、iam、system）。
     */
    String product() default "";

    /**
     * 绑定到本权限组的角色码列表。
     */
    String[] roles() default {};

    /**
     * 组内排序，越小越靠前。
     */
    int sort() default 0;
}
