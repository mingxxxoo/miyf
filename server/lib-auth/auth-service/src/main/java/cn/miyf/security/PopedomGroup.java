package cn.miyf.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器级权限组声明。启动扫描写入权限组，方法级权限挂到本组。
 * <p>
 * {@code value} 为 8 位权限组编码。请勿在业务代码中手写数字，优先使用模块组合注解：
 * {@link IamAdminPopedom}、{@link SystemSettingsPopedom}，或各业务模块的 {@code *Popedom} /
 * {@code *PopedomCodes}。
 * <p>
 * 方法权限生成 16 位 ID：{@code value + 8位序号}，根权限为 {@code value + 00000000}。
 *
 * @author XieMingJie
 * @see docs/AUTH_PERMISSION_PLAN.md
 * @since 2026-09-06
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
     * 权限组名称（展示用，如「个人」「单位」「管理员」）。
     */
    String name() default "";

    /**
     * 产品/业务域（展示树第二层，如 kitchen、health）。
     */
    String product() default "";

    /**
     * 组内排序，越小越靠前。
     */
    int sort() default 0;
}
