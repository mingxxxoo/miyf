package cn.miyf.health.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.bean.dto.HealthSampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSubjectEntity;
import cn.miyf.health.bean.vo.HealthOverviewVo;
import cn.miyf.health.bean.vo.HealthProviderVo;
import cn.miyf.health.bean.vo.HealthTrendVo;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.provider.ManualHealthDataProvider;
import cn.miyf.health.provider.huawei.HuaweiHealthOAuthService;
import cn.miyf.health.repository.HealthProviderBindingRepository;
import cn.miyf.health.repository.HealthSampleRepository;
import cn.miyf.health.repository.HealthSubjectRepository;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthDataProviderRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 健康 CRUD 应用服务：主体 / 采样 / 绑定 / 概览与趋势。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Service
public class HealthCrudApplicationService {

    private static final Set<String> GENDERS = Set.of("UNKNOWN", "MALE", "FEMALE");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");
    private static final Set<String> QUALITIES = Set.of("NORMAL", "ESTIMATED", "SUSPECT");

    private final HealthProperties healthProperties;
    private final HealthDataProviderRegistry providerRegistry;
    private final HealthSubjectRepository subjectRepository;
    private final HealthSampleRepository sampleRepository;
    private final HealthProviderBindingRepository bindingRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final HuaweiHealthOAuthService huaweiHealthOAuthService;

    /**
     * 构造 CRUD 服务。
     *
     * @param healthProperties          配置
     * @param providerRegistry          数据源注册表
     * @param subjectRepository         主体仓储
     * @param sampleRepository          采样仓储
     * @param bindingRepository         绑定仓储
     * @param snowflakeIdGenerator      雪花 ID
     * @param huaweiHealthOAuthService  华为 OAuth（删除主体时清 token）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthCrudApplicationService(HealthProperties healthProperties,
                                        HealthDataProviderRegistry providerRegistry,
                                        HealthSubjectRepository subjectRepository,
                                        HealthSampleRepository sampleRepository,
                                        HealthProviderBindingRepository bindingRepository,
                                        SnowflakeIdGenerator snowflakeIdGenerator,
                                        HuaweiHealthOAuthService huaweiHealthOAuthService) {
        this.healthProperties = healthProperties;
        this.providerRegistry = providerRegistry;
        this.subjectRepository = subjectRepository;
        this.sampleRepository = sampleRepository;
        this.bindingRepository = bindingRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.huaweiHealthOAuthService = huaweiHealthOAuthService;
    }

    /**
     * 模块概览。
     *
     * @return VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthOverviewVo overview() {
        return new HealthOverviewVo()
                .setEnabled(healthProperties.isEnabled())
                .setSubjectCount(subjectRepository.count())
                .setSampleCount(sampleRepository.count())
                .setProviders(listProviders());
    }

    /**
     * 数据源列表。
     *
     * @return VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthProviderVo> listProviders() {
        return providerRegistry.all().stream().map(this::toProviderVo).toList();
    }

    /**
     * 主体列表。
     *
     * @param keyword 关键字
     * @return 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSubjectEntity> listSubjects(String keyword) {
        return subjectRepository.listByKeyword(keyword);
    }

    /**
     * 创建主体。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectEntity createSubject(HealthSubjectSaveDto dto) {
        Instant now = Instant.now();
        HealthSubjectEntity entity = mapSubject(new HealthSubjectEntity(), dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        subjectRepository.insert(entity);
        return entity;
    }

    /**
     * 更新主体。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectEntity updateSubject(Long id, HealthSubjectSaveDto dto) {
        HealthSubjectEntity entity = requireSubject(id);
        mapSubject(entity, dto);
        entity.setLastModifyTime(Instant.now());
        subjectRepository.updateById(entity);
        return entity;
    }

    /**
     * 删除主体（级联采样由 FK CASCADE；绑定显式删除并清华为 token）。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteSubject(Long id) {
        requireSubject(id);
        huaweiHealthOAuthService.clearToken(id);
        bindingRepository.deleteBySubjectId(id);
        subjectRepository.deleteById(id);
    }

    /**
     * 采样列表。
     *
     * @param subjectId  主体
     * @param metricCode 指标
     * @param limit      条数上限
     * @return 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSampleEntity> listSamples(Long subjectId, String metricCode, Integer limit) {
        int rows = limit == null || limit < 1 ? 100 : Math.min(limit, 500);
        return sampleRepository.list(subjectId, metricCode, rows);
    }

    /**
     * 指标时间序列趋势（按测量时间升序）。
     *
     * @param subjectId  主体
     * @param metricCode 指标
     * @param from       起始（可选）
     * @param to         结束（可选）
     * @param limit      点数上限
     * @return 趋势
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthTrendVo trend(Long subjectId, String metricCode, Instant from, Instant to, Integer limit) {
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "subjectId 不能为空");
        }
        if (!StringUtils.hasText(metricCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "metricCode 不能为空");
        }
        requireSubject(subjectId);
        String metric = metricCode.trim().toUpperCase(Locale.ROOT);
        int rows = limit == null || limit < 1 ? 200 : Math.min(limit, 1000);
        List<HealthSampleEntity> samples = sampleRepository.listTrend(subjectId, metric, from, to, rows);

        HealthTrendVo vo = new HealthTrendVo()
                .setSubjectId(String.valueOf(subjectId))
                .setMetricCode(metric)
                .setUnit(HealthMetricCodes.defaultUnit(metric))
                .setPointCount(samples.size());

        BigDecimal sum = BigDecimal.ZERO;
        int valueCount = 0;
        for (HealthSampleEntity sample : samples) {
            if (StringUtils.hasText(sample.getUnit())) {
                vo.setUnit(sample.getUnit());
            }
            BigDecimal value = sample.getValueNum();
            HealthTrendVo.Point point = new HealthTrendVo.Point()
                    .setMeasuredTime(sample.getMeasuredTime())
                    .setValue(value)
                    .setProviderCode(sample.getProviderCode())
                    .setQuality(sample.getQuality());
            vo.getPoints().add(point);
            if (value == null) {
                continue;
            }
            sum = sum.add(value);
            valueCount++;
            if (vo.getMin() == null || value.compareTo(vo.getMin()) < 0) {
                vo.setMin(value);
            }
            if (vo.getMax() == null || value.compareTo(vo.getMax()) > 0) {
                vo.setMax(value);
            }
            vo.setLatest(value);
            vo.setLatestTime(sample.getMeasuredTime());
        }
        if (valueCount > 0) {
            vo.setAvg(sum.divide(BigDecimal.valueOf(valueCount), 4, RoundingMode.HALF_UP));
        }
        return vo;
    }

    /**
     * 手动录入采样。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSampleEntity createManualSample(HealthSampleSaveDto dto) {
        Long subjectId = parseId(dto.getSubjectId(), "主体 ID");
        requireSubject(subjectId);
        String metric = dto.getMetricCode().trim().toUpperCase(Locale.ROOT);
        String unit = StringUtils.hasText(dto.getUnit()) ? dto.getUnit().trim() : HealthMetricCodes.defaultUnit(metric);
        String quality = normalizeQuality(dto.getQuality());
        Instant now = Instant.now();
        HealthSampleEntity entity = new HealthSampleEntity()
                .setSubjectId(subjectId)
                .setMetricCode(metric)
                .setValueNum(dto.getValueNum())
                .setUnit(unit)
                .setMeasuredTime(dto.getMeasuredTime())
                .setProviderCode(ManualHealthDataProvider.CODE)
                .setSourceSampleId(null)
                .setQuality(quality)
                .setMetaJson(dto.getMetaJson());
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        sampleRepository.insert(entity);
        return entity;
    }

    /**
     * 删除采样。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteSample(Long id) {
        HealthSampleEntity entity = sampleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "采样不存在"));
        sampleRepository.deleteById(entity.getId());
    }

    /**
     * 列出主体的数据源绑定。
     *
     * @param subjectId 主体
     * @return 绑定列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthProviderBindingEntity> listBindings(Long subjectId) {
        requireSubject(subjectId);
        return bindingRepository.listBySubjectId(subjectId);
    }

    /**
     * 创建或更新数据源绑定。
     *
     * @param subjectId 主体
     * @param dto       请求
     * @return 绑定
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthProviderBindingEntity upsertBinding(Long subjectId, HealthProviderBindingSaveDto dto) {
        requireSubject(subjectId);
        String providerCode = dto.getProviderCode().trim();
        providerRegistry.find(providerCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "健康数据源不存在: " + providerCode));
        String status = StringUtils.hasText(dto.getStatus())
                ? dto.getStatus().trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if (!Set.of("ACTIVE", "INACTIVE", "REVOKED").contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的绑定状态");
        }
        Instant now = Instant.now();
        return bindingRepository.findBySubjectAndProvider(subjectId, providerCode)
                .map(existing -> {
                    existing.setExternalAccountId(dto.getExternalAccountId());
                    existing.setCredentialRef(dto.getCredentialRef());
                    existing.setStatus(status);
                    existing.setLastModifyTime(now);
                    bindingRepository.updateById(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    HealthProviderBindingEntity entity = new HealthProviderBindingEntity()
                            .setSubjectId(subjectId)
                            .setProviderCode(providerCode)
                            .setExternalAccountId(dto.getExternalAccountId())
                            .setCredentialRef(dto.getCredentialRef())
                            .setStatus(status);
                    entity.setId(snowflakeIdGenerator.nextId());
                    entity.setCreateTime(now);
                    entity.setLastModifyTime(now);
                    bindingRepository.insert(entity);
                    return entity;
                });
    }

    /**
     * 校验并返回健康主体（供同步等编排服务复用）。
     *
     * @param id 主体 ID
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSubjectEntity requireSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "健康主体不存在"));
    }

    private HealthProviderVo toProviderVo(HealthDataProvider provider) {
        return new HealthProviderVo()
                .setCode(provider.code())
                .setDisplayName(provider.displayName())
                .setEnabled(provider.enabled())
                .setSupportsRemoteFetch(provider.supportsRemoteFetch())
                .setSupportedMetrics(provider.supportedMetrics());
    }

    private HealthSubjectEntity mapSubject(HealthSubjectEntity entity, HealthSubjectSaveDto dto) {
        return entity
                .setDisplayName(dto.getDisplayName().trim())
                .setGender(normalizeGender(dto.getGender()))
                .setBirthDate(dto.getBirthDate())
                .setHeightCm(dto.getHeightCm())
                .setExternalUserId(parseOptionalId(dto.getExternalUserId()))
                .setStatus(normalizeStatus(dto.getStatus()))
                .setRemark(dto.getRemark());
    }

    private Long parseId(String raw, String label) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "非法");
        }
    }

    private Long parseOptionalId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "外部用户 ID 非法");
        }
    }

    private String normalizeGender(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
        if (!GENDERS.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的性别");
        }
        return v;
    }

    private String normalizeStatus(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!STATUSES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的状态");
        }
        return v;
    }

    private String normalizeQuality(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "NORMAL";
        if (!QUALITIES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的采样质量");
        }
        return v;
    }
}
