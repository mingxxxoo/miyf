package cn.miyf.health.provider;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.provider.huawei.HuaweiHealthApiClient;
import cn.miyf.health.provider.huawei.HuaweiHealthOAuthService;
import cn.miyf.health.provider.huawei.HuaweiTokenBundle;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 华为运动健康（Health Kit）数据源。
 * <p>
 * 默认关闭；开启后需配置 {@code app.health.huawei.*}，并通过 OAuth 为每个主体授权。
 * {@code mockEnabled=true} 且主体未授权时返回演示数据。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class HuaweiHealthDataProvider implements HealthDataProvider {

    public static final String CODE = "huawei";

    private final HealthProperties healthProperties;
    private final HuaweiHealthOAuthService oauthService;
    private final HuaweiHealthApiClient apiClient;

    public HuaweiHealthDataProvider(HealthProperties healthProperties,
                                    HuaweiHealthOAuthService oauthService,
                                    HuaweiHealthApiClient apiClient) {
        this.healthProperties = healthProperties;
        this.oauthService = oauthService;
        this.apiClient = apiClient;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "华为运动健康";
    }

    @Override
    public boolean enabled() {
        return healthProperties.getProviders()
                .getOrDefault(CODE, new HealthProperties.ProviderConfig())
                .isEnabled();
    }

    @Override
    public Set<String> supportedMetrics() {
        return Set.of(
                HealthMetricCodes.WEIGHT,
                HealthMetricCodes.HEIGHT,
                HealthMetricCodes.BODY_FAT,
                HealthMetricCodes.HEART_RATE,
                HealthMetricCodes.STEPS,
                HealthMetricCodes.BLOOD_PRESSURE_SYS,
                HealthMetricCodes.BLOOD_PRESSURE_DIA,
                HealthMetricCodes.BLOOD_GLUCOSE
        );
    }

    @Override
    public List<HealthSampleDraft> fetch(HealthFetchRequest request) {
        if (request.getSubjectId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        Instant to = request.getTo() != null ? request.getTo() : Instant.now();
        Instant from = request.getFrom() != null ? request.getFrom() : to.minus(7, ChronoUnit.DAYS);
        // mock 仅在该主体未 OAuth 授权时生效；已授权则拉真实数据
        boolean hasCred = oauthService.hasToken(request.getSubjectId());
        if (healthProperties.getHuawei().isMockEnabled() && !hasCred) {
            return mockFetch(request.getSubjectId(), from, to, request.getMetricCodes());
        }
        HuaweiTokenBundle token = oauthService.requireAccessToken(request.getSubjectId());
        if (!StringUtils.hasText(token.getAccessToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "华为 access_token 为空");
        }
        return apiClient.fetchSamples(
                request.getSubjectId(),
                token.getAccessToken(),
                healthProperties.getHuawei().getClientId(),
                from,
                to,
                request.getMetricCodes());
    }

    private List<HealthSampleDraft> mockFetch(Long subjectId, Instant from, Instant to, Set<String> metrics) {
        List<HealthSampleDraft> drafts = new ArrayList<>();
        Instant cursor = from.truncatedTo(ChronoUnit.DAYS);
        int day = 0;
        while (!cursor.isAfter(to) && day < 14) {
            String key = cursor.toString();
            drafts.add(new HealthSampleDraft()
                    .setSubjectId(subjectId)
                    .setMetricCode(HealthMetricCodes.WEIGHT)
                    .setValueNum(BigDecimal.valueOf(70.0 - day * 0.05))
                    .setUnit(HealthMetricCodes.defaultUnit(HealthMetricCodes.WEIGHT))
                    .setMeasuredTime(cursor.plus(7, ChronoUnit.HOURS))
                    .setSourceSampleId("huawei-mock-weight-" + subjectId + "-" + key)
                    .setQuality("ESTIMATED"));
            drafts.add(new HealthSampleDraft()
                    .setSubjectId(subjectId)
                    .setMetricCode(HealthMetricCodes.HEART_RATE)
                    .setValueNum(BigDecimal.valueOf(68 + (day % 5)))
                    .setUnit(HealthMetricCodes.defaultUnit(HealthMetricCodes.HEART_RATE))
                    .setMeasuredTime(cursor.plus(8, ChronoUnit.HOURS))
                    .setSourceSampleId("huawei-mock-hr-" + subjectId + "-" + key)
                    .setQuality("ESTIMATED"));
            drafts.add(new HealthSampleDraft()
                    .setSubjectId(subjectId)
                    .setMetricCode(HealthMetricCodes.STEPS)
                    .setValueNum(BigDecimal.valueOf(7500 + day * 120L))
                    .setUnit(HealthMetricCodes.defaultUnit(HealthMetricCodes.STEPS))
                    .setMeasuredTime(cursor.plus(21, ChronoUnit.HOURS))
                    .setSourceSampleId("huawei-mock-steps-" + subjectId + "-" + key)
                    .setQuality("ESTIMATED"));
            cursor = cursor.plus(1, ChronoUnit.DAYS);
            day++;
        }
        if (metrics != null && !metrics.isEmpty()) {
            return drafts.stream().filter(d -> metrics.contains(d.getMetricCode())).toList();
        }
        return drafts;
    }
}
