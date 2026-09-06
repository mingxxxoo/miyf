package cn.miyf.config;

import cn.miyf.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.miyf.common.ApiResult;
import cn.miyf.common.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 过滤链：无状态 JWT；细粒度权限由方法级 AuthorizationManager / {@code @PreAuthorize} 控制。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /**
     * 构造安全配置。
     *
     * @param jwtAuthFilter JWT 过滤器
     * @param objectMapper  JSON 工具
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * 安全过滤链。
     *
     * @param http HttpSecurity
     * @return 过滤链
     * @throws Exception 配置异常
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 登录与文档、健康检查、静态上传放行
                        .requestMatchers(
                                "/api/auth/wx-login",
                                "/api/admin/auth/login",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/uploads/**",
                                "/druid/**"
                        ).permitAll()
                        // 管理端 / IAM 需认证；细粒度权限由 Spring Security 方法鉴权控制
                        .requestMatchers("/api/admin/**", "/api/iam/**").authenticated()
                        // 上传需登录，细粒度权限由方法注解控制
                        .requestMatchers("/api/upload").authenticated()
                        // 用户写操作需登录；公开浏览在控制器层可不强制登录
                        .requestMatchers("/api/orders/**", "/api/comments/**", "/api/user/**").authenticated()
                        .requestMatchers("/api/**").permitAll()
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
     * 写出统一错误 JSON。
     *
     * @param response  响应
     * @param errorCode 错误码
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    private void writeJson(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(errorCode));
    }
}
