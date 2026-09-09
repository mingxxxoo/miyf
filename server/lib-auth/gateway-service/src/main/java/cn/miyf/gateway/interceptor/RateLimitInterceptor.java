package cn.miyf.gateway.interceptor;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.gateway.cache.GatewayCacheKeys;
import cn.miyf.infrastructure.redis.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 固定窗口限流拦截器（按登录用户或 IP）。
 * 写操作与高风险路径使用严格限流；普通 GET 允许 Redis 故障降级。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter redisRateLimiter;
    private final RedisAppProperties properties;

    /**
     * 请求进入前做限流检查。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否继续
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return true;
        }
        String identity = resolveIdentity(request);
        String key = GatewayCacheKeys.apiRate(identity);
        int max = properties.getRateLimit().getApiMaxRequests();
        long window = properties.getRateLimit().getApiWindowSeconds();
        if (isStrictPath(request)) {
            redisRateLimiter.checkAndIncrementStrict(key, max, window);
        } else {
            redisRateLimiter.checkAndIncrement(key, max, window);
        }
        return true;
    }

    /**
     * 写方法与上传/OAuth/登录等路径走严格限流。
     *
     * @param request 请求
     * @return true 严格
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private boolean isStrictPath(HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if (!"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method)) {
            return true;
        }
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI().toLowerCase();
        return uri.contains("/upload")
                || uri.contains("/oauth")
                || uri.contains("/login")
                || uri.contains("/captcha");
    }

    /**
     * 优先已登录主体，其次客户端 IP。
     *
     * @param request 请求
     * @return 身份标识
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    private String resolveIdentity(HttpServletRequest request) {
        return SecurityUtils.currentPrincipal()
                .filter(p -> p.getId() != null)
                .map(p -> p.getType() + ":" + p.getId())
                .orElseGet(() -> {
                    String forwarded = request.getHeader("X-Forwarded-For");
                    if (forwarded != null && !forwarded.isBlank()) {
                        return "ip:" + forwarded.split(",")[0].trim();
                    }
                    return "ip:" + (request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr());
                });
    }
}
