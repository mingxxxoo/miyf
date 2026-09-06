package cn.miyf.health.domain;

/**
 * 规范指标编码（厂商无关 canonical code）。
 * <p>
 * 外部数据源映射到这些编码后再入库；扩展新指标时优先在此登记。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public final class HealthMetricCodes {

    public static final String WEIGHT = "WEIGHT";
    public static final String HEIGHT = "HEIGHT";
    public static final String BMI = "BMI";
    public static final String BODY_FAT = "BODY_FAT";
    public static final String HEART_RATE = "HEART_RATE";
    public static final String STEPS = "STEPS";
    public static final String SLEEP_MINUTES = "SLEEP_MINUTES";
    public static final String BLOOD_PRESSURE_SYS = "BLOOD_PRESSURE_SYS";
    public static final String BLOOD_PRESSURE_DIA = "BLOOD_PRESSURE_DIA";
    public static final String BLOOD_GLUCOSE = "BLOOD_GLUCOSE";

    private HealthMetricCodes() {
    }

    /**
     * 默认单位。
     *
     * @param metricCode 指标编码
     * @return 单位
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public static String defaultUnit(String metricCode) {
        if (metricCode == null) {
            return "";
        }
        return switch (metricCode) {
            case WEIGHT -> "kg";
            case HEIGHT -> "cm";
            case BMI -> "kg/m2";
            case BODY_FAT -> "%";
            case HEART_RATE -> "bpm";
            case STEPS -> "count";
            case SLEEP_MINUTES -> "min";
            case BLOOD_PRESSURE_SYS, BLOOD_PRESSURE_DIA -> "mmHg";
            case BLOOD_GLUCOSE -> "mmol/L";
            default -> "";
        };
    }
}
