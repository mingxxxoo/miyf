package cn.miyf.interceptor;

import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.redis.CacheKeys;
import cn.miyf.infrastructure.redis.RedisRateLimiter;
import cn.miyf.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 固定窗口限流拦截器（按登录用户或 IP）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter redisRateLimiter;
    private final RedisAppProperties properties;

    /**
     * 构造拦截器。
     *
     * @param redisRateLimiter 限流器
     * @param properties       配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public RateLimitInterceptor(RedisRateLimiter redisRateLimiter, RedisAppProperties properties) {
        this.redisRateLimiter = redisRateLimiter;
        this.properties = properties;
    }

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
        redisRateLimiter.checkAndIncrement(
                CacheKeys.apiRate(identity),
                properties.getRateLimit().getApiMaxRequests(),
                properties.getRateLimit().getApiWindowSeconds()
        );
        return true;
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

