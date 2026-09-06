package cn.miyf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * OSS 本地磁盘模式：将存储目录映射到 /uploads/**。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Configuration
public class OssWebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public OssWebMvcConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
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
