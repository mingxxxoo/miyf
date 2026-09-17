package cn.miyf.ai.support;

import cn.miyf.ai.config.AiProperties;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页正文抓取：仅 http/https，SSRF 防护，清洗后截断。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Component
public class WebPageFetcher {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?is)<title[^>]*>\\s*(.*?)\\s*</title>");
    private static final Pattern SCRIPT_STYLE = Pattern.compile(
            "(?is)<(script|style|nav|footer|header|aside|noscript)[^>]*>.*?</\\1>");
    private static final Pattern TAGS = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern MULTI_NL = Pattern.compile("\\n{3,}");

    private final RestClient aiWebRestClient;
    private final AiProperties aiProperties;

    /**
     * @param aiWebRestClient 网页抓取客户端
     * @param aiProperties    AI 配置
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public WebPageFetcher(@Qualifier("aiWebRestClient") RestClient aiWebRestClient,
                          AiProperties aiProperties) {
        this.aiWebRestClient = aiWebRestClient;
        this.aiProperties = aiProperties;
    }

    /**
     * 抓取并清洗网页正文。
     *
     * @param rawUrl 用户输入 URL
     * @return 标题与正文
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public FetchedPage fetch(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供网页链接");
        }
        URI uri = validateUri(rawUrl.trim());
        String html;
        try {
            html = aiWebRestClient.get()
                    .uri(uri)
                    .accept(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.ALL)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "链接读取失败，可以粘贴文字试试");
        }
        if (!StringUtils.hasText(html)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "链接读取失败，可以粘贴文字试试");
        }
        String title = extractTitle(html);
        String cleaned = cleanHtml(html);
        int maxChars = aiProperties.getWebFetch().getMaxChars();
        if (cleaned.length() > maxChars) {
            cleaned = cleaned.substring(0, maxChars);
        }
        return new FetchedPage(title, uri.toString(), cleaned);
    }

    private URI validateUri(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "链接格式不正确");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 http/https 链接");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "链接缺少主机名");
        }
        assertPublicHost(uri.getHost());
        return uri;
    }

    private void assertPublicHost(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许访问内网或保留地址");
                }
            }
        } catch (UnknownHostException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析链接主机");
        }
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            return TAGS.matcher(matcher.group(1)).replaceAll("").trim();
        }
        return "";
    }

    private String cleanHtml(String html) {
        String withoutBlocks = SCRIPT_STYLE.matcher(html).replaceAll(" ");
        String text = TAGS.matcher(withoutBlocks).replaceAll(" ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        text = WHITESPACE.matcher(text).replaceAll(" ");
        text = text.replace('\u00a0', ' ');
        text = MULTI_NL.matcher(text.replace("\r\n", "\n")).replaceAll("\n\n");
        return text.trim();
    }

    /**
     * 抓取结果。
     *
     * @param title    页面标题
     * @param finalUrl 最终 URL
     * @param text     清洗正文
     */
    public record FetchedPage(String title, String finalUrl, String text) {
    }
}
