package cn.miyf.gateway.config;

import cn.miyf.common.ApiResult;
import cn.miyf.common.ErrorCode;
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
 * {@code /api/**} 采用显式白名单：公开浏览与登录放行，写操作与管理端需认证，其余 API 需认证。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final boolean exposeDocs;

    /**
     * 构造安全配置。
     *
     * @param jwtAuthFilter JWT 过滤器
     * @param objectMapper  JSON 工具
     * @param exposeDocs    是否放行 Swagger（生产应关闭）
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     * @history 1.01 2026-09-08 XieMingJie /api 显式白名单；文档入口可关。
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          ObjectMapper objectMapper,
                          @Value("${app.security.expose-docs:true}") boolean exposeDocs) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
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
     *
     * @param http HttpSecurity
     * @return 过滤链
     * @throws Exception 配置异常
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     * @history 1.01 2026-09-08 XieMingJie 显式白名单，去掉 /api/** 兜底 permitAll。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> publicPaths = new ArrayList<>(List.of(
                "/api/auth/wx-login",
                "/api/admin/auth/login",
                "/api/admin/auth/login-status",
                "/api/admin/auth/captcha",
                "/actuator/health",
                "/actuator/info",
                "/uploads/**",
                "/api/categories/**",
                "/api/dishes/**"
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
                        .requestMatchers("/api/admin/**", "/api/iam/**").authenticated()
                        .requestMatchers("/api/upload").authenticated()
                        .requestMatchers("/api/orders/**", "/api/comments/**", "/api/user/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
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
