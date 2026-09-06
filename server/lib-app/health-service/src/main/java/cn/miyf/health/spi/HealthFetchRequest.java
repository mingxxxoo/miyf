package cn.miyf.health.spi;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * 向外部健康数据源拉取采样的请求。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class HealthFetchRequest {

    private Long subjectId;
    private String externalAccountId;
    private Instant from;
    private Instant to;
    private Set<String> metricCodes = Collections.emptySet();

    public Long getSubjectId() {
        return subjectId;
    }

    public HealthFetchRequest setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
        return this;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public HealthFetchRequest setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
        return this;
    }

    public Instant getFrom() {
        return from;
    }

    public HealthFetchRequest setFrom(Instant from) {
        this.from = from;
        return this;
    }

    public Instant getTo() {
        return to;
    }

    public HealthFetchRequest setTo(Instant to) {
        this.to = to;
        return this;
    }

    public Set<String> getMetricCodes() {
        return metricCodes;
    }

    public HealthFetchRequest setMetricCodes(Set<String> metricCodes) {
        this.metricCodes = metricCodes == null ? Collections.emptySet() : metricCodes;
        return this;
    }
}
