package cn.miyf.health.controller.personal;

import cn.miyf.common.BusinessException;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.provider.huawei.HuaweiHealthAuthFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人端华为 OAuth 浏览器回跳入口（公开）。
 * 华为授权完成后 GET 带回 code/state，本接口换票落库后跳转成功页或配置的成功 URL。
 * 须在 Security 白名单放行，且 userRedirectUri 与华为控制台登记完全一致。
 *
 * @author XieMingJie
 * @since 2026-09-11
 * @history 1.00 2026-09-11 XieMingJie Created.
 */
@Tag(name = "用户端-健康-华为授权回调")
@RestController
@RequestMapping("/health/providers/huawei/oauth")
@RequiredArgsConstructor
@Slf4j
public class HuaweiHealthOAuthRedirectController {

    private final HuaweiHealthAuthFacade huaweiHealthAuthFacade;
    private final HealthProperties healthProperties;

    /**
     * 华为 OAuth 授权完成后的浏览器回调。
     * 成功则绑定 ACTIVE；失败或用户拒绝则展示错误页。无需登录会话（凭一次性 state）。
     *
     * @param code             授权码（成功时）
     * @param state            OAuth state
     * @param error            华为拒绝时的错误码
     * @param errorDescription 错误描述
     * @param response         HTTP 响应（重定向或 HTML）
     * @throws IOException 写出响应失败
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Operation(summary = "华为 OAuth 浏览器回跳换票")
    @GetMapping("/redirect")
    public void redirect(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         @RequestParam(name = "error_description", required = false) String errorDescription,
                         HttpServletResponse response) throws IOException {
        if (StringUtils.hasText(error)) {
            String msg = StringUtils.hasText(errorDescription) ? errorDescription : error;
            writeHtml(response, false, "华为授权未完成", escape(msg) + "。请返回小程序后重试。");
            return;
        }
        try {
            Map<String, Object> result = huaweiHealthAuthFacade.completeOAuthFromRedirect(code, state);
            String successUri = healthProperties.getHuawei().getUserSuccessRedirectUri();
            if (StringUtils.hasText(successUri)) {
                String targetBase = successUri.trim();
                if (!isAllowedSuccessRedirect(targetBase)) {
                    log.warn("huawei success redirect blocked: {}", targetBase);
                    writeHtml(response, true, "华为授权成功",
                            "已绑定华为健康数据。请返回小程序，下拉刷新或点击「同步」拉取数据。");
                    return;
                }
                String sep = targetBase.contains("?") ? "&" : "?";
                String target = targetBase
                        + sep
                        + "authorized=1"
                        + "&subjectId=" + enc(String.valueOf(result.get("subjectId")));
                response.sendRedirect(target);
                return;
            }
            writeHtml(response, true, "华为授权成功",
                    "已绑定华为健康数据。请返回小程序，下拉刷新或点击「同步」拉取数据。");
        } catch (BusinessException ex) {
            log.warn("huawei user oauth redirect failed: {}", ex.getMessage());
            writeHtml(response, false, "华为授权失败", escape(ex.getMessage()) + "。请返回小程序重新发起授权。");
        } catch (Exception ex) {
            log.warn("huawei user oauth redirect error: {}", ex.getMessage());
            writeHtml(response, false, "华为授权失败", "系统异常，请返回小程序重试。");
        }
    }

    private boolean isAllowedSuccessRedirect(String raw) {
        try {
            URI uri = URI.create(raw);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme) && !"http".equals(scheme)) {
                return false;
            }
            // 生产建议仅 https；本地联调允许 http + localhost
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            String hostLower = host.toLowerCase(Locale.ROOT);
            Set<String> allowed = healthProperties.getHuawei().getUserSuccessRedirectHosts().stream()
                    .filter(StringUtils::hasText)
                    .map(h -> h.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (allowed.isEmpty()) {
                // 未配置白名单：仅允许 localhost / 127.0.0.1，避免开放重定向
                return "localhost".equals(hostLower) || "127.0.0.1".equals(hostLower);
            }
            return allowed.contains(hostLower);
        } catch (Exception ex) {
            return false;
        }
    }

    private static void writeHtml(HttpServletResponse response, boolean ok, String title, String body)
            throws IOException {
        response.setStatus(ok ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        String color = ok ? "#0d7a4f" : "#b42318";
        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>%s</title>
                  <style>
                    body{font-family:system-ui,sans-serif;margin:0;padding:48px 24px;
                         background:#f6f8f7;color:#1a1a1a;text-align:center}
                    h1{font-size:22px;color:%s;margin:0 0 12px}
                    p{font-size:15px;line-height:1.6;color:#555;margin:0 auto;max-width:420px}
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(escape(title), color, escape(title), body);
        response.getWriter().write(html);
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
