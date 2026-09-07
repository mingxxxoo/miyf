package cn.miyf.health.provider;

import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 示例外部数据源：演示可插拔接入，默认关闭；不绑定任何真实厂商。
 * <p>
 * 真实厂商实现可复制本类结构：读配置 → 调 SDK/HTTP → 映射为 {@link HealthSampleDraft}。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class ExampleExternalHealthDataProvider implements HealthDataProvider {

    public static final String CODE = "example";

    private final HealthProperties healthProperties;

    public ExampleExternalHealthDataProvider(HealthProperties healthProperties) {
        this.healthProperties = healthProperties;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "示例数据源";
    }

    @Override
    public boolean enabled() {
        return healthProperties.getProviders().getOrDefault(CODE, new HealthProperties.ProviderConfig()).isEnabled();
    }

    @Override
    public Set<String> supportedMetrics() {
        return Set.of(HealthMetricCodes.WEIGHT, HealthMetricCodes.HEART_RATE, HealthMetricCodes.STEPS);
    }

    @Override
    public List<HealthSampleDraft> fetch(HealthFetchRequest request) {
        if (request.getSubjectId() == null) {
            return List.of();
        }
        Instant to = request.getTo() != null ? request.getTo() : Instant.now();
        Instant from = request.getFrom() != null ? request.getFrom() : to.minus(3, ChronoUnit.DAYS);
        List<HealthSampleDraft> drafts = new ArrayList<>();
        Instant cursor = from.truncatedTo(ChronoUnit.DAYS);
        int day = 0;
        while (!cursor.isAfter(to) && day < 7) {
            String dayKey = cursor.toString();
            drafts.add(new HealthSampleDraft()
                    .setSubjectId(request.getSubjectId())
                    .setMetricCode(HealthMetricCodes.WEIGHT)
                    .setValueNum(BigDecimal.valueOf(68.0 + day * 0.1))
                    .setUnit(HealthMetricCodes.defaultUnit(HealthMetricCodes.WEIGHT))
                    .setMeasuredTime(cursor.plus(8, ChronoUnit.HOURS))
                    .setSourceSampleId("example-weight-" + request.getSubjectId() + "-" + dayKey)
                    .setQuality("ESTIMATED"));
            drafts.add(new HealthSampleDraft()
                    .setSubjectId(request.getSubjectId())
                    .setMetricCode(HealthMetricCodes.STEPS)
                    .setValueNum(BigDecimal.valueOf(6000 + day * 300L))
                    .setUnit(HealthMetricCodes.defaultUnit(HealthMetricCodes.STEPS))
                    .setMeasuredTime(cursor.plus(20, ChronoUnit.HOURS))
                    .setSourceSampleId("example-steps-" + request.getSubjectId() + "-" + dayKey)
                    .setQuality("ESTIMATED"));
            cursor = cursor.plus(1, ChronoUnit.DAYS);
            day++;
        }
        if (request.getMetricCodes() != null && !request.getMetricCodes().isEmpty()) {
            return drafts.stream()
                    .filter(d -> request.getMetricCodes().contains(d.getMetricCode()))
                    .toList();
        }
        return drafts;
    }
}
