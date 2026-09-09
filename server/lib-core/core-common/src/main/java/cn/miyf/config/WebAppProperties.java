package cn.miyf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Web 层通用配置（前缀 {@code app.web}）。
 * <p>
 * 业务 Controller 的 {@code @RequestMapping} 只写业务路径（如 {@code /dishes}），
 * 由 {@code api-prefix} 统一追加全局前缀（默认 {@code /api}）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@ConfigurationProperties(prefix = "app.web")
public class WebAppProperties {

    /**
     * 业务 API 全局前缀；空字符串表示不加前缀。
     */
    private String apiPrefix = "/api";

    /**
     * @return 原始配置值
     */
    public String getApiPrefix() {
        return apiPrefix;
    }

    /**
     * @param apiPrefix 全局前缀
     */
    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    /**
     * 规范化后的前缀：空表示无前缀；否则以 {@code /} 开头且不以 {@code /} 结尾。
     *
     * @return 规范化前缀
     */
    public String normalizedApiPrefix() {
        if (!StringUtils.hasText(apiPrefix)) {
            return "";
        }
        String p = apiPrefix.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return "/".equals(p) ? "" : p;
    }

    /**
     * 将相对路径拼到全局 API 前缀后。
     * <p>
     * 例：前缀 {@code /api} 时，{@code api("/dishes/**")} → {@code /api/dishes/**}；
     * 前缀为空时原样返回规范化相对路径。
     *
     * @param relativePath 相对路径（建议以 {@code /} 开头）
     * @return 带前缀的完整路径
     */
    public String api(String relativePath) {
        String rel = relativePath == null ? "" : relativePath.trim();
        if (!StringUtils.hasText(rel)) {
            String prefix = normalizedApiPrefix();
            return StringUtils.hasText(prefix) ? prefix : "/";
        }
        if (!rel.startsWith("/")) {
            rel = "/" + rel;
        }
        String prefix = normalizedApiPrefix();
        return StringUtils.hasText(prefix) ? prefix + rel : rel;
    }
}
