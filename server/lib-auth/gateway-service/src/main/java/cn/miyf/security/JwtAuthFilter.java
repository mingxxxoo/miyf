package cn.miyf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：从 Authorization Bearer 解析并写入 SecurityContext。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * 构造过滤器。
     *
     * @param jwtService JWT 服务
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 解析 Token；无效 Token 不阻断，交由后续授权规则处理。
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 链
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (!token.isEmpty()) {
                    try {
                        AuthPrincipal principal = jwtService.parseToken(token);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        // 同步写入 ThreadLocal，供业务层通过 LoginUserContext / LocalThreadMap 读取
                        LoginUserContext.set(principal);
                    } catch (Exception ignored) {
                        // Token 无效时清空上下文，避免脏认证
                        SecurityContextHolder.clearContext();
                        LoginUserContext.clear();
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // 容器线程池会复用线程，必须清理 ThreadLocal
            LoginUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
