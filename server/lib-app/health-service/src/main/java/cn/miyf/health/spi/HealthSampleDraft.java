package cn.miyf.health.spi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 数据源拉取后的规范采样草稿（入库前）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public class HealthSampleDraft {

    private Long subjectId;
    private String metricCode;
    private BigDecimal valueNum;
    private String unit;
    private Instant measuredAt;
    private String sourceSampleId;
    private String quality = "NORMAL";
    private Map<String, Object> meta;

    public Long getSubjectId() {
        return subjectId;
    }

    public HealthSampleDraft setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
        return this;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public HealthSampleDraft setMetricCode(String metricCode) {
        this.metricCode = metricCode;
        return this;
    }

    public BigDecimal getValueNum() {
        return valueNum;
    }

    public HealthSampleDraft setValueNum(BigDecimal valueNum) {
        this.valueNum = valueNum;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public HealthSampleDraft setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public HealthSampleDraft setMeasuredAt(Instant measuredAt) {
        this.measuredAt = measuredAt;
        return this;
    }

    public String getSourceSampleId() {
        return sourceSampleId;
    }

    public HealthSampleDraft setSourceSampleId(String sourceSampleId) {
        this.sourceSampleId = sourceSampleId;
        return this;
    }

    public String getQuality() {
        return quality;
    }

    public HealthSampleDraft setQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public HealthSampleDraft setMeta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }
}
