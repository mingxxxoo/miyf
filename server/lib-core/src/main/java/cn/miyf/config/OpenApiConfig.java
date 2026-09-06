package cn.miyf.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger 配置：文档信息、JWT 安全方案、用户端/管理端分组。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * 定义文档元信息、服务器地址与 Bearer JWT。
     *
     * @return OpenAPI
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("clumsy_kitchen API")
                        .description("""
                                笨拙厨房 — 菜品预约与管理系统。

                                **产品定位**：家庭/小团队内部预约做饭，不涉及金额、支付与商业交易。
                                **版本**：1.0.0 · **时区**：Asia/Shanghai（UTC+8）
                                **鉴权**：`Authorization: Bearer <JWT>`（登录接口除外）
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("clumsy_kitchen").email("dev@clumsys.local"))
                        .license(new License().name("Non-Commercial Internal Use").url("https://example.local/license")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目 README")
                        .url("https://github.com/example/clumsy_kitchen"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地后端直连"),
                        new Server().url("http://localhost").description("Docker Compose + Nginx")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("管理员或用户登录后获得的 JWT")));
    }

    /**
     * 用户端 / 公开浏览相关接口分组。
     *
     * @return 分组
     * @history 1.00 2026-09-05 09:28 XieMingJie Created.
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("01-user")
                .displayName("用户端")
                .pathsToMatch("/api/auth/**", "/api/categories/**", "/api/dishes/**",
                        "/api/orders/**", "/api/comments/**", "/api/user/**")
                .build();
    }

    /**
     * 管理端与上传接口分组。
     *
     * @return 分组
     * @history 1.00 2026-09-05 09:28 XieMingJie Created.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("02-admin")
                .displayName("管理端")
                .pathsToMatch("/api/admin/**", "/api/upload")
                .build();
    }
}
