package cn.miyf.gateway.config;

import cn.miyf.common.ApiResult;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.WebAppProperties;
import cn.miyf.gateway.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 过滤链：无状态 JWT；细粒度权限由方法级 AuthorizationManager / {@code @PreAuthorize} 控制。
 * <p>
 * 业务 API（{@code app.web.api-prefix}，默认 {@code /api/**}）采用显式白名单：
 * 公开浏览与登录放行，写操作与管理端需认证，其余 API 需认证。
 * 路径统一经 {@code app.web.api-prefix} 组装；含华为个人端 OAuth 浏览器回跳放行。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 * @history 1.00 2026-09-04 17:06 XieMingJie Created.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final WebAppProperties webAppProperties;
    private final boolean exposeDocs;

    /**
     * 构造安全配置。
     * 路径改读 {@code app.web.api-prefix}。
     *
     * @param jwtAuthFilter    JWT 过滤器
     * @param objectMapper     JSON 工具
     * @param webAppProperties Web 配置（API 前缀）
     * @param exposeDocs       是否放行 Swagger（默认关闭）
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          ObjectMapper objectMapper,
                          WebAppProperties webAppProperties,
                          @Value("${app.security.expose-docs:false}") boolean exposeDocs) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
        this.webAppProperties = webAppProperties;
        this.exposeDocs = exposeDocs;
    }

    /**
     * 禁用 Boot 默认内存用户（避免生成开发密码日志）；认证走 JWT。
     *
     * @return UserDetailsService
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("JWT only: " + username);
        };
    }

    /**
     * 安全过滤链。
     * 公开路径含登录、健康检查、浏览 API 及华为个人端 OAuth 浏览器回跳；路径经 api-prefix 组装。
     *
     * @param http HttpSecurity
     * @return 过滤链
     * @throws Exception 配置异常
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> publicPaths = new ArrayList<>(List.of(
                webAppProperties.api("/auth/wx-login"),
                webAppProperties.api("/admin/auth/login"),
                webAppProperties.api("/admin/auth/login-status"),
                webAppProperties.api("/admin/auth/captcha"),
                // 华为个人端 OAuth 浏览器回跳（无会话，凭一次性 state 换票）
                webAppProperties.api("/health/providers/huawei/oauth/redirect"),
                "/actuator/health",
                "/actuator/info",
                "/error",
                "/r/**",
                webAppProperties.api("/categories/**"),
                webAppProperties.api("/dishes/**")
        ));
        if (exposeDocs) {
            publicPaths.add("/v3/api-docs/**");
            publicPaths.add("/swagger-ui/**");
            publicPaths.add("/swagger-ui.html");
        }
        String[] publics = publicPaths.toArray(String[]::new);

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publics).permitAll()
                        .requestMatchers(
                                webAppProperties.api("/admin/**"),
                                webAppProperties.api("/iam/**")
                        ).authenticated()
                        .requestMatchers(webAppProperties.api("/upload")).authenticated()
                        .requestMatchers(
                                webAppProperties.api("/orders/**"),
                                webAppProperties.api("/comments/**"),
                                webAppProperties.api("/user/**"),
                                webAppProperties.api("/health/**")
                        ).authenticated()
                        .requestMatchers(webAppProperties.api("/**")).authenticated()
                        // 未显式放行的路径一律拒绝，避免非 /api 路径默认可访问
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, ErrorCode.FORBIDDEN))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 写出统一错误 JSON；鉴权类错误使用对应 HTTP 状态（401/403），便于前端拦截器识别。
     *
     * @param response  响应
     * @param errorCode 错误码
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    private void writeJson(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        int httpStatus = HttpServletResponse.SC_OK;
        if (errorCode == ErrorCode.UNAUTHORIZED) {
            httpStatus = HttpServletResponse.SC_UNAUTHORIZED;
        } else if (errorCode == ErrorCode.FORBIDDEN) {
            httpStatus = HttpServletResponse.SC_FORBIDDEN;
        }
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(errorCode));
    }
}
