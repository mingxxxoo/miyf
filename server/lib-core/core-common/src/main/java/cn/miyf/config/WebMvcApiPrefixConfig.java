package cn.miyf.config;

import cn.miyf.web.RootMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为业务 Controller 统一追加 {@code app.web.api-prefix}。
 * <p>
 * Controller 上不要再手写 {@code /api}；需要挂在根路径的接口请标注 {@link RootMapping}。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Configuration
@EnableConfigurationProperties(WebAppProperties.class)
@RequiredArgsConstructor
public class WebMvcApiPrefixConfig implements WebMvcConfigurer {

    private final WebAppProperties webAppProperties;

    /**
     * 对 {@link RestController}/{@link Controller} 追加全局前缀，排除根映射与 Advice。
     *
     * @param configurer 路径匹配配置
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        String prefix = webAppProperties.normalizedApiPrefix();
        if (!StringUtils.hasText(prefix)) {
            return;
        }
        configurer.addPathPrefix(prefix, type ->
                (AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                        || AnnotatedElementUtils.hasAnnotation(type, Controller.class))
                        && !AnnotatedElementUtils.hasAnnotation(type, RootMapping.class)
                        && !AnnotatedElementUtils.hasAnnotation(type, RestControllerAdvice.class));
    }
}
