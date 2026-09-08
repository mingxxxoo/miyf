package cn.miyf.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索模块配置属性（前缀 {@code app.search}）。
 * 负责开关、索引前缀、节点地址、可选账号与超时；业务通过 {@link #resolveIndex(String)} 得到物理索引名。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.search")
public class SearchAppProperties {

    /**
     * 是否启用 Elasticsearch；关闭时使用 Noop 实现，不影响启动。
     */
    private boolean enabled = false;

    /**
     * 索引名前缀，如 miyf → miyf_kitchen_dish。
     */
    private String indexPrefix = "miyf";

    /**
     * ES 节点 URI 列表，例如 http://localhost:9200。
     */
    private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

    /**
     * 可选用户名（集群开启安全认证时使用）。
     */
    private String username = "";

    /**
     * 可选密码。
     */
    private String password = "";

    /**
     * 连接超时（毫秒）。
     */
    private int connectTimeoutMs = 3000;

    /**
     * 响应/Socket 超时（毫秒）。
     */
    private int socketTimeoutMs = 10000;

    /**
     * 将逻辑索引名解析为带前缀的物理索引名。
     * 已带前缀则原样返回，避免重复拼接。
     *
     * @param logicalName 逻辑名，如 kitchen_dish
     * @return 物理索引名，如 miyf_kitchen_dish
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String resolveIndex(String logicalName) {
        if (logicalName == null || logicalName.isBlank()) {
            throw new IllegalArgumentException("index logicalName 不能为空");
        }
        String name = logicalName.trim();
        String prefix = indexPrefix == null ? "" : indexPrefix.trim();
        if (prefix.isEmpty()) {
            return name;
        }
        // 调用方若已传入物理名，不再二次加前缀
        if (name.startsWith(prefix + "_")) {
            return name;
        }
        return prefix + "_" + name;
    }
}
