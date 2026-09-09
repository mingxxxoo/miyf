package cn.miyf.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不追加 {@code app.web.api-prefix} 的 Controller（映射挂在站点根路径）。
 * <p>
 * 典型场景：公开文件读取 {@code GET /r/{fileId}}，避免变成 {@code /api/r/...}。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RootMapping {
}
