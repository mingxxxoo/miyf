package cn.miyf.oss.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注查询/展示 VO 中的资源文件字段（URL、文件 ID 或 ID 列表）。
 * <p>
 * 响应写出时由 {@link FileAccessResponseAdvice} 抽取文件 ID，写入当前主体的 Redis 临时访问权；
 * {@code GET /r/{fileId}} 据此放行。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileAccess {
}
