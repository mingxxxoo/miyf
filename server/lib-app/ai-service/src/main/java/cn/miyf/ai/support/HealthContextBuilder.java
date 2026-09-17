package cn.miyf.ai.support;

import cn.miyf.health.bean.vo.HealthSampleVo;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.service.HealthCrudApplicationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户近 30 天异常指标 → prompt 上下文（最小化字段，脱敏后可落日志）。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Component
@RequiredArgsConstructor
public class HealthContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(HealthContextBuilder.class);

    private static final Map<String, AlertRule> RULES;

    static {
        Map<String, AlertRule> rules = new LinkedHashMap<>();
        rules.put(HealthMetricCodes.WEIGHT, new AlertRule(35d, 150d, "体重", "35-150"));
        rules.put(HealthMetricCodes.HEART_RATE, new AlertRule(50d, 100d, "心率", "50-100"));
        rules.put(HealthMetricCodes.BLOOD_PRESSURE_SYS, new AlertRule(90d, 139d, "收缩压", "90-139"));
        rules.put(HealthMetricCodes.BLOOD_PRESSURE_DIA, new AlertRule(60d, 89d, "舒张压", "60-89"));
        rules.put(HealthMetricCodes.BLOOD_GLUCOSE, new AlertRule(3.9d, 7.8d, "血糖", "3.9-7.8"));
        rules.put(HealthMetricCodes.BODY_FAT, new AlertRule(8d, 35d, "体脂", "8-35"));
        rules.put(HealthMetricCodes.BMI, new AlertRule(18.5d, 27.9d, "BMI", "18.5-27.9"));
        rules.put(HealthMetricCodes.SLEEP_MINUTES, new AlertRule(300d, 600d, "睡眠分钟", "300-600"));
        rules.put(HealthMetricCodes.STRESS, new AlertRule(null, 79d, "压力", "≤79"));
        RULES = Collections.unmodifiableMap(rules);
    }

    private final HealthCrudApplicationService healthCrudApplicationService;
    private final ObjectMapper objectMapper;

    /**
     * 构建近 30 天异常指标 JSON；无权或失败时返回 []。
     *
     * @return JSON 数组字符串
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public String buildAlertsJson() {
        List<Map<String, Object>> alerts = listAlerts();
        try {
            return objectMapper.writeValueAsString(alerts);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 列出近 30 天各指标最新异常值（每种指标最多一条）。
     *
     * @return 异常列表
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public List<Map<String, Object>> listAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        try {
            List<HealthSampleVo> samples = healthCrudApplicationService.listMySamples(null, 200);
            Map<String, HealthSampleVo> latestByMetric = new LinkedHashMap<>();
            for (HealthSampleVo sample : samples) {
                if (sample.getMeasuredTime() == null || sample.getMeasuredTime().isBefore(since)) {
                    continue;
                }
                if (sample.getValueNum() == null || !StringUtils.hasText(sample.getMetricCode())) {
                    continue;
                }
                String code = HealthMetricCodes.normalize(sample.getMetricCode());
                latestByMetric.putIfAbsent(code, sample);
            }
            for (Map.Entry<String, HealthSampleVo> entry : latestByMetric.entrySet()) {
                AlertRule rule = RULES.get(entry.getKey());
                if (rule == null) {
                    continue;
                }
                BigDecimal value = entry.getValue().getValueNum();
                String level = assess(value.doubleValue(), rule);
                if (level == null) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("metric", rule.displayName());
                row.put("latest", value);
                row.put("refRange", rule.refRange());
                row.put("level", level);
                alerts.add(row);
            }
        } catch (Exception ex) {
            log.debug("构建健康异常指标失败，降级为空: {}", ex.toString());
        }
        return alerts;
    }

    private String assess(double value, AlertRule rule) {
        if (rule.low() != null && value < rule.low()) {
            return "low";
        }
        if (rule.high() != null && value > rule.high()) {
            return "high";
        }
        return null;
    }

    private record AlertRule(Double low, Double high, String displayName, String refRange) {
    }
}
