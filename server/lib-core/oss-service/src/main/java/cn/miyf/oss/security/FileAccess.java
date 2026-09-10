package cn.miyf.oss.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注查询/展示 VO 中的资源文件字段（URL、文件 ID 或 ID 列表）。
 * <p>
 * 响应写出时由 {@link FileAccessResponseAdvice}：
 * <ul>
 *   <li>将字段改写为带 {@code exp}/{@code sig} 的短期签名 URL（img / 小程序 Image 可直开）；</li>
 *   <li>若当前有登录主体，另写入 Redis 临时访问权（兼容带 JWT 的下载）。</li>
 * </ul>
 * {@code GET /r/{fileId}} 校验签名或既有权限后放行；该路径不做 Spring Security JWT 强制鉴权。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileAccess {
}
