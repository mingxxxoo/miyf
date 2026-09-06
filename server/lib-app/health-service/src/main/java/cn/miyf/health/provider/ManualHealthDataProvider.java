package cn.miyf.health.provider;

import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 手动录入数据源：不拉远程，仅作为 provider_code=manual 的入库来源标识。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class ManualHealthDataProvider implements HealthDataProvider {

    public static final String CODE = "manual";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "手动录入";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean supportsRemoteFetch() {
        return false;
    }

    @Override
    public Set<String> supportedMetrics() {
        return Set.of(
                HealthMetricCodes.WEIGHT,
                HealthMetricCodes.HEIGHT,
                HealthMetricCodes.BMI,
                HealthMetricCodes.BODY_FAT,
                HealthMetricCodes.HEART_RATE,
                HealthMetricCodes.STEPS,
                HealthMetricCodes.SLEEP_MINUTES,
                HealthMetricCodes.BLOOD_PRESSURE_SYS,
                HealthMetricCodes.BLOOD_PRESSURE_DIA,
                HealthMetricCodes.BLOOD_GLUCOSE
        );
    }

    @Override
    public List<HealthSampleDraft> fetch(HealthFetchRequest request) {
        return Collections.emptyList();
    }
}
