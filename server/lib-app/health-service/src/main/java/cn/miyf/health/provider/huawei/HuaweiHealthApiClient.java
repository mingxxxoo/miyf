package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.spi.HealthSampleDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 华为 Health Kit REST：采样聚合查询与指标映射。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
@Slf4j
public class HuaweiHealthApiClient {

    /**
     * 规范指标 → 华为 dataTypeName
     */
    private static final Map<String, String> METRIC_TO_HUAWEI = Map.of(
            HealthMetricCodes.WEIGHT, "com.huawei.instantaneous.body_weight",
            HealthMetricCodes.HEIGHT, "com.huawei.instantaneous.height",
            HealthMetricCodes.BODY_FAT, "com.huawei.instantaneous.body.fat.rate",
            HealthMetricCodes.HEART_RATE, "com.huawei.instantaneous.heart_rate",
            HealthMetricCodes.STEPS, "com.huawei.continuous.steps.delta",
            HealthMetricCodes.BLOOD_PRESSURE_SYS, "com.huawei.instantaneous.blood_pressure",
            HealthMetricCodes.BLOOD_PRESSURE_DIA, "com.huawei.instantaneous.blood_pressure",
            HealthMetricCodes.BLOOD_GLUCOSE, "com.huawei.instantaneous.blood_glucose"
    );

    private final HealthProperties healthProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * 构造 Health Kit 客户端。
     *
     * @param healthProperties 配置（API base 等）
     * @param objectMapper     JSON
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HuaweiHealthApiClient(HealthProperties healthProperties,
                                 ObjectMapper objectMapper,
                                 @org.springframework.beans.factory.annotation.Qualifier("huaweiRestClient") RestClient huaweiRestClient) {
        this.healthProperties = healthProperties;
        this.objectMapper = objectMapper;
        this.restClient = huaweiRestClient;
    }

    /**
     * 聚合查询华为采样并映射为草稿。
     *
     * @param subjectId     主体
     * @param accessToken   access_token
     * @param clientId      x-client-id，可空则用配置
     * @param from          起始
     * @param to            结束
     * @param metricFilter  指标过滤，可空
     * @return 草稿列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSampleDraft> fetchSamples(Long subjectId,
                                                String accessToken,
                                                String clientId,
                                                Instant from,
                                                Instant to,
                                                Set<String> metricFilter) {
        List<String> dataTypes = resolveDataTypes(metricFilter);
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode polymerizeWith = body.putArray("polymerizeWith");
        for (String type : dataTypes) {
            polymerizeWith.addObject().put("dataTypeName", type);
        }
        body.put("startTime", from.toEpochMilli());
        body.put("endTime", to.toEpochMilli());

        String xClientId = StringUtils.hasText(clientId)
                ? clientId
                : healthProperties.getHuawei().getClientId();
        String url = trimSlash(healthProperties.getHuawei().getHealthApiBase()) + "/sampleSet:polymerize";
        try {
            var spec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken);
            if (StringUtils.hasText(xClientId)) {
                spec = spec.header("x-client-id", xClientId);
            }
            String resp = spec.body(body).retrieve().body(String.class);
            return parsePolymerizeResponse(subjectId, resp == null ? "{}" : resp, metricFilter);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("huawei polymerize failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "拉取华为健康数据失败: " + ex.getMessage());
        }
    }

    private List<String> resolveDataTypes(Set<String> metricFilter) {
        Map<String, String> selected = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : METRIC_TO_HUAWEI.entrySet()) {
            if (metricFilter == null || metricFilter.isEmpty() || metricFilter.contains(e.getKey())) {
                selected.put(e.getValue(), e.getValue());
            }
        }
        if (selected.isEmpty()) {
            selected.put(METRIC_TO_HUAWEI.get(HealthMetricCodes.WEIGHT),
                    METRIC_TO_HUAWEI.get(HealthMetricCodes.WEIGHT));
        }
        return new ArrayList<>(selected.keySet());
    }

    private List<HealthSampleDraft> parsePolymerizeResponse(Long subjectId, String json, Set<String> metricFilter) {
        List<HealthSampleDraft> drafts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("error") || root.has("errorCode")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "华为返回错误: " + root.path("errorMessage").asText(root.path("message").asText("unknown")));
            }
            JsonNode groups = root.path("group");
            if (!groups.isArray()) {
                // 兼容 samplePoints 直出结构
                collectFromSamplePointsNode(subjectId, root.path("samplePoints"), drafts, metricFilter);
                return filter(drafts, metricFilter);
            }
            for (JsonNode group : groups) {
                JsonNode sampleSets = group.path("sampleSet");
                if (!sampleSets.isArray()) {
                    continue;
                }
                for (JsonNode set : sampleSets) {
                    String dataType = set.path("dataTypeName").asText("");
                    JsonNode points = set.path("samplePoints");
                    if (!points.isArray()) {
                        continue;
                    }
                    for (JsonNode point : points) {
                        drafts.addAll(mapPoint(subjectId, dataType, point));
                    }
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析华为采样响应失败");
        }
        return filter(drafts, metricFilter);
    }

    private void collectFromSamplePointsNode(Long subjectId, JsonNode samplePoints,
                                             List<HealthSampleDraft> out, Set<String> metricFilter) {
        if (samplePoints == null || samplePoints.isMissingNode()) {
            return;
        }
        if (samplePoints.isObject()) {
            samplePoints.fields().forEachRemaining(e -> {
                JsonNode point = e.getValue();
                String dataType = point.path("dataTypeName").asText(e.getKey());
                out.addAll(mapPoint(subjectId, dataType, point));
            });
        }
    }

    private List<HealthSampleDraft> mapPoint(Long subjectId, String dataType, JsonNode point) {
        List<HealthSampleDraft> list = new ArrayList<>();
        Instant measuredTime = parseHuaweiTime(point.path("endTime").asLong(0),
                point.path("startTime").asLong(0));
        if (measuredTime == null) {
            measuredTime = Instant.now();
        }
        String sourceBase = dataType + ":" + measuredTime.toEpochMilli();
        JsonNode values = point.path("value");
        if (!values.isArray()) {
            return list;
        }
        if ("com.huawei.instantaneous.body_weight".equals(dataType)) {
            BigDecimal weight = findFloat(values, "body_weight");
            if (weight != null) {
                list.add(draft(subjectId, HealthMetricCodes.WEIGHT, weight, measuredTime, sourceBase + ":weight"));
            }
            BigDecimal fat = findFloat(values, "body_fat_rate");
            if (fat == null) {
                fat = findFloat(values, "fat_rate");
            }
            if (fat != null) {
                list.add(draft(subjectId, HealthMetricCodes.BODY_FAT, fat, measuredTime, sourceBase + ":fat"));
            }
        } else if ("com.huawei.instantaneous.height".equals(dataType)) {
            BigDecimal height = findFloat(values, "height");
            if (height != null) {
                // 华为多为米，规范用 cm
                if (height.compareTo(BigDecimal.TEN) < 0) {
                    height = height.multiply(BigDecimal.valueOf(100));
                }
                list.add(draft(subjectId, HealthMetricCodes.HEIGHT, height, measuredTime, sourceBase + ":height"));
            }
        } else if ("com.huawei.instantaneous.body.fat.rate".equals(dataType)) {
            BigDecimal fat = findFloat(values, "body_fat_rate");
            if (fat != null) {
                list.add(draft(subjectId, HealthMetricCodes.BODY_FAT, fat, measuredTime, sourceBase + ":fat"));
            }
        } else if ("com.huawei.instantaneous.heart_rate".equals(dataType)) {
            BigDecimal hr = findFloat(values, "bpm");
            if (hr == null) {
                hr = findNumber(values, "bpm");
            }
            if (hr != null) {
                list.add(draft(subjectId, HealthMetricCodes.HEART_RATE, hr, measuredTime, sourceBase + ":hr"));
            }
        } else if ("com.huawei.continuous.steps.delta".equals(dataType)) {
            BigDecimal steps = findNumber(values, "steps_delta");
            if (steps != null) {
                list.add(draft(subjectId, HealthMetricCodes.STEPS, steps, measuredTime, sourceBase + ":steps"));
            }
        } else if ("com.huawei.instantaneous.blood_pressure".equals(dataType)) {
            BigDecimal sys = findFloat(values, "systolic_pressure");
            BigDecimal dia = findFloat(values, "diastolic_pressure");
            if (sys != null) {
                list.add(draft(subjectId, HealthMetricCodes.BLOOD_PRESSURE_SYS, sys, measuredTime, sourceBase + ":sys"));
            }
            if (dia != null) {
                list.add(draft(subjectId, HealthMetricCodes.BLOOD_PRESSURE_DIA, dia, measuredTime, sourceBase + ":dia"));
            }
        } else if ("com.huawei.instantaneous.blood_glucose".equals(dataType)) {
            BigDecimal glucose = findFloat(values, "level");
            if (glucose == null) {
                glucose = findFloat(values, "blood_glucose");
            }
            if (glucose != null) {
                list.add(draft(subjectId, HealthMetricCodes.BLOOD_GLUCOSE, glucose, measuredTime, sourceBase + ":bg"));
            }
        }
        return list;
    }

    private HealthSampleDraft draft(Long subjectId, String metric, BigDecimal value,
                                    Instant measuredTime, String sourceId) {
        return new HealthSampleDraft()
                .setSubjectId(subjectId)
                .setMetricCode(metric)
                .setValueNum(value)
                .setUnit(HealthMetricCodes.defaultUnit(metric))
                .setMeasuredTime(measuredTime)
                .setSourceSampleId(sourceId)
                .setQuality("NORMAL")
                .setMeta(Map.of("provider", "huawei"));
    }

    private static List<HealthSampleDraft> filter(List<HealthSampleDraft> drafts, Set<String> metricFilter) {
        if (metricFilter == null || metricFilter.isEmpty()) {
            return drafts;
        }
        return drafts.stream().filter(d -> metricFilter.contains(d.getMetricCode())).toList();
    }

    private static BigDecimal findFloat(JsonNode values, String field) {
        for (JsonNode v : values) {
            if (field.equals(v.path("fieldName").asText())) {
                if (v.has("floatValue") && !v.get("floatValue").isNull()) {
                    return BigDecimal.valueOf(v.get("floatValue").asDouble());
                }
            }
        }
        return null;
    }

    private static BigDecimal findNumber(JsonNode values, String field) {
        for (JsonNode v : values) {
            if (field.equals(v.path("fieldName").asText())) {
                if (v.has("integerValue") && !v.get("integerValue").isNull()) {
                    return BigDecimal.valueOf(v.get("integerValue").asLong());
                }
                if (v.has("floatValue") && !v.get("floatValue").isNull()) {
                    return BigDecimal.valueOf(v.get("floatValue").asDouble());
                }
            }
        }
        return null;
    }

    /**
     * 华为时间多为纳秒；也兼容毫秒。
     */
    private static Instant parseHuaweiTime(long end, long start) {
        long raw = end > 0 ? end : start;
        if (raw <= 0) {
            return null;
        }
        if (raw > 10_000_000_000_000_000L) {
            // nanoseconds
            return Instant.ofEpochMilli(raw / 1_000_000L);
        }
        if (raw > 10_000_000_000L) {
            // milliseconds
            return Instant.ofEpochMilli(raw);
        }
        return Instant.ofEpochSecond(raw);
    }

    private static String trimSlash(String base) {
        if (!StringUtils.hasText(base)) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
