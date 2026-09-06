package cn.miyf.monitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 组件监控快照。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class ComponentHealthSnapshot {

    private String name;
    private String status;
    private Map<String, Object> details = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public ComponentHealthSnapshot setName(String name) {
        this.name = name;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ComponentHealthSnapshot setStatus(String status) {
        this.status = status;
        return this;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public ComponentHealthSnapshot put(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
