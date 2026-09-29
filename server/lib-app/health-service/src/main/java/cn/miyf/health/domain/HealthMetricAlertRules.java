package cn.miyf.health.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指标参考区间与告警文案（与小程序 / AI HealthContextBuilder 对齐）。
 *
 * @author XieMingJie
 * @since 2026-09-29
 */
public final class HealthMetricAlertRules {

    public record AlertRule(Double low, Double high, String label, String lowMessage, String highMessage) {
    }

    public record AlertHit(String metricCode, String label, String level, String message) {
    }

    private static final Map<String, AlertRule> RULES;

    static {
        Map<String, AlertRule> rules = new LinkedHashMap<>();
        rules.put(HealthMetricCodes.WEIGHT, new AlertRule(35d, 150d, "体重", "体重偏低", "体重偏高"));
        rules.put(HealthMetricCodes.HEART_RATE, new AlertRule(50d, 100d, "心率", "心率偏低", "心率偏高"));
        rules.put(HealthMetricCodes.BLOOD_PRESSURE_SYS, new AlertRule(90d, 139d, "收缩压", "收缩压偏低", "收缩压偏高"));
        rules.put(HealthMetricCodes.BLOOD_PRESSURE_DIA, new AlertRule(60d, 89d, "舒张压", "舒张压偏低", "舒张压偏高"));
        rules.put(HealthMetricCodes.BLOOD_GLUCOSE, new AlertRule(3.9d, 7.8d, "血糖", "血糖偏低", "血糖偏高"));
        rules.put(HealthMetricCodes.BODY_FAT, new AlertRule(8d, 35d, "体脂", "体脂偏低", "体脂偏高"));
        rules.put(HealthMetricCodes.BMI, new AlertRule(18.5d, 27.9d, "BMI", "BMI 偏低", "BMI 偏高"));
        rules.put(HealthMetricCodes.SLEEP_MINUTES, new AlertRule(300d, 600d, "睡眠", "睡眠偏少", "睡眠偏长"));
        rules.put(HealthMetricCodes.STRESS, new AlertRule(null, 79d, "压力", "压力偏低", "压力偏高"));
        RULES = Collections.unmodifiableMap(rules);
    }

    private HealthMetricAlertRules() {
    }

    public static Map<String, AlertRule> rules() {
        return RULES;
    }

    public static String labelOf(String metricCode) {
        AlertRule rule = RULES.get(HealthMetricCodes.normalize(metricCode));
        return rule != null ? rule.label() : metricCode;
    }

    /**
     * @return 告警命中，正常则 null
     */
    public static AlertHit assess(String metricCode, double value) {
        AlertRule rule = RULES.get(HealthMetricCodes.normalize(metricCode));
        if (rule == null) {
            return null;
        }
        if (rule.low() != null && value < rule.low()) {
            return new AlertHit(HealthMetricCodes.normalize(metricCode), rule.label(), "low", rule.lowMessage());
        }
        if (rule.high() != null && value > rule.high()) {
            return new AlertHit(HealthMetricCodes.normalize(metricCode), rule.label(), "high", rule.highMessage());
        }
        return null;
    }
}
