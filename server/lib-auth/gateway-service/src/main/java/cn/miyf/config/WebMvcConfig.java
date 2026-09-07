package cn.miyf.config;

import cn.miyf.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网关 Web MVC：API 限流拦截器。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 构造配置（本地上传静态映射已迁至 oss-service）。
     *
     * @param rateLimitInterceptor 限流拦截器
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * 仅对 /api/** 限流，排除文档与健康检查。
     *
     * @param registry 拦截器注册表
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/wx-login",
                        "/api/admin/auth/login",
                        "/api/admin/auth/login-status",
                        "/api/admin/auth/captcha",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                );
    }
}
