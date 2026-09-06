package cn.miyf.config;

import cn.miyf.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web MVC：限流拦截器 + 本地上传静态映射。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final FileStorageProperties fileStorageProperties;

    /**
     * 构造配置。
     *
     * @param rateLimitInterceptor   限流拦截器
     * @param fileStorageProperties  文件存储配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        FileStorageProperties fileStorageProperties) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.fileStorageProperties = fileStorageProperties;
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
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                );
    }

    /**
     * 将本地存储目录映射到 /uploads/**。
     *
     * @param registry 资源注册表
     * @history 1.00 2026-09-05 09:19 XieMingJie Created.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 仅本地磁盘模式由本机提供 /uploads；MinIO 走对象存储公网/直链 URL
        if (!"local".equalsIgnoreCase(fileStorageProperties.getType())) {
            return;
        }
        Path root = Path.of(fileStorageProperties.getPath()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
