package cn.miyf.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式定时任务元数据（配合 {@link org.springframework.scheduling.annotation.Scheduled}）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MiyfJob {

    String code();

    String name() default "";

    String description() default "";
}
