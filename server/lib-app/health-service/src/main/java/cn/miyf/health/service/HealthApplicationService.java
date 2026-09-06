package cn.miyf.health.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.bean.dto.HealthSampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSubjectEntity;
import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.bean.vo.HealthOverviewVo;
import cn.miyf.health.bean.vo.HealthProviderVo;
import cn.miyf.health.bean.vo.HealthTrendVo;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.provider.ManualHealthDataProvider;
import cn.miyf.health.provider.huawei.HuaweiHealthOAuthService;
import cn.miyf.health.repository.mapper.HealthProviderBindingMapper;
import cn.miyf.health.repository.mapper.HealthSampleMapper;
import cn.miyf.health.repository.mapper.HealthSubjectMapper;
import cn.miyf.health.repository.mapper.HealthSyncRunMapper;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthDataProviderRegistry;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 健康应用服务：主体/采样/同步编排；数据源细节委托 {@link HealthDataProvider}。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class HealthApplicationService {

    private static final Set<String> GENDERS = Set.of("UNKNOWN", "MALE", "FEMALE");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");
    private static final Set<String> QUALITIES = Set.of("NORMAL", "ESTIMATED", "SUSPECT");

    private final HealthProperties healthProperties;
    private final HealthDataProviderRegistry providerRegistry;
    private final HealthSubjectMapper subjectMapper;
    private final HealthSampleMapper sampleMapper;
    private final HealthProviderBindingMapper bindingMapper;
    private final HealthSyncRunMapper syncRunMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;
    private final HuaweiHealthOAuthService huaweiHealthOAuthService;

    public HealthApplicationService(HealthProperties healthProperties,
                                    HealthDataProviderRegistry providerRegistry,
                                    HealthSubjectMapper subjectMapper,
                                    HealthSampleMapper sampleMapper,
                                    HealthProviderBindingMapper bindingMapper,
                                    HealthSyncRunMapper syncRunMapper,
                                    SnowflakeIdGenerator snowflakeIdGenerator,
                                    ObjectMapper objectMapper,
                                    HuaweiHealthOAuthService huaweiHealthOAuthService) {
        this.healthProperties = healthProperties;
        this.providerRegistry = providerRegistry;
        this.subjectMapper = subjectMapper;
        this.sampleMapper = sampleMapper;
        this.bindingMapper = bindingMapper;
        this.syncRunMapper = syncRunMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.objectMapper = objectMapper;
        this.huaweiHealthOAuthService = huaweiHealthOAuthService;
    }

    /**
     * 模块概览。
     *
     * @return VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public HealthOverviewVo overview() {
        return new HealthOverviewVo()
                .setEnabled(healthProperties.isEnabled())
                .setSubjectCount(subjectMapper.selectCount(Wrappers.<HealthSubjectEntity>lambdaQuery()))
                .setSampleCount(sampleMapper.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery()))
                .setProviders(listProviders());
    }

    /**
     * 数据源列表。
     *
     * @return VO 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthProviderVo> listProviders() {
        return providerRegistry.all().stream().map(this::toProviderVo).toList();
    }

    /**
     * 主体列表。
     *
     * @param keyword 关键字
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthSubjectEntity> listSubjects(String keyword) {
        return subjectMapper.selectList(Wrappers.<HealthSubjectEntity>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(HealthSubjectEntity::getDisplayName, keyword)
                        .or()
                        .like(HealthSubjectEntity::getRemark, keyword))
                .orderByDesc(HealthSubjectEntity::getUpdatedAt));
    }

    /**
     * 创建主体。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectEntity createSubject(HealthSubjectSaveDto dto) {
        Instant now = Instant.now();
        HealthSubjectEntity entity = mapSubject(new HealthSubjectEntity(), dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        subjectMapper.insert(entity);
        return entity;
    }

    /**
     * 更新主体。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectEntity updateSubject(Long id, HealthSubjectSaveDto dto) {
        HealthSubjectEntity entity = requireSubject(id);
        mapSubject(entity, dto);
        entity.setUpdatedAt(Instant.now());
        subjectMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除主体（级联采样）。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void deleteSubject(Long id) {
        requireSubject(id);
        huaweiHealthOAuthService.clearToken(id);
        bindingMapper.delete(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, id));
        subjectMapper.deleteById(id);
    }

    /**
     * 采样列表。
     *
     * @param subjectId  主体
     * @param metricCode 指标
     * @param limit      条数上限
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthSampleEntity> listSamples(Long subjectId, String metricCode, Integer limit) {
        int rows = limit == null || limit < 1 ? 100 : Math.min(limit, 500);
        return sampleMapper.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(subjectId != null, HealthSampleEntity::getSubjectId, subjectId)
                .eq(StringUtils.hasText(metricCode), HealthSampleEntity::getMetricCode, metricCode)
                .orderByDesc(HealthSampleEntity::getMeasuredAt)
                .last("LIMIT " + rows));
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
     * @history 1.00 2026-09-06 XieMingJie Created.
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
        List<HealthSampleEntity> samples = sampleMapper.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(HealthSampleEntity::getSubjectId, subjectId)
                .eq(HealthSampleEntity::getMetricCode, metric)
                .ge(from != null, HealthSampleEntity::getMeasuredAt, from)
                .le(to != null, HealthSampleEntity::getMeasuredAt, to)
                .orderByAsc(HealthSampleEntity::getMeasuredAt)
                .last("LIMIT " + rows));

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
                    .setMeasuredAt(sample.getMeasuredAt())
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
            vo.setLatestAt(sample.getMeasuredAt());
        }
        if (valueCount > 0) {
            vo.setAvg(sum.divide(BigDecimal.valueOf(valueCount), 4, java.math.RoundingMode.HALF_UP));
        }
        return vo;
    }

    /**
     * 手动录入采样。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
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
                .setMeasuredAt(dto.getMeasuredAt())
                .setProviderCode(ManualHealthDataProvider.CODE)
                .setSourceSampleId(null)
                .setQuality(quality)
                .setMetaJson(dto.getMetaJson());
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        sampleMapper.insert(entity);
        return entity;
    }

    /**
     * 删除采样。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void deleteSample(Long id) {
        HealthSampleEntity entity = sampleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采样不存在");
        }
        sampleMapper.deleteById(id);
    }

    /**
     * 列出主体的数据源绑定。
     *
     * @param subjectId 主体
     * @return 绑定列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthProviderBindingEntity> listBindings(Long subjectId) {
        requireSubject(subjectId);
        return bindingMapper.selectList(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .orderByAsc(HealthProviderBindingEntity::getProviderCode));
    }

    /**
     * 列出可远程拉取的 ACTIVE 绑定（供定时同步）。
     *
     * @return 绑定列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthProviderBindingEntity> listActiveRemoteBindings() {
        List<HealthProviderBindingEntity> rows = bindingMapper.selectList(
                Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                        .eq(HealthProviderBindingEntity::getStatus, "ACTIVE")
                        .orderByAsc(HealthProviderBindingEntity::getProviderCode)
                        .orderByAsc(HealthProviderBindingEntity::getSubjectId));
        return rows.stream()
                .filter(row -> providerRegistry.find(row.getProviderCode())
                        .filter(p -> p.enabled() && p.supportsRemoteFetch())
                        .isPresent())
                .toList();
    }

    /**
     * 创建或更新数据源绑定。
     *
     * @param subjectId 主体
     * @param dto       请求
     * @return 绑定
     * @history 1.00 2026-09-06 XieMingJie Created.
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
        HealthProviderBindingEntity existing = bindingMapper.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .last("LIMIT 1"));
        if (existing == null) {
            HealthProviderBindingEntity entity = new HealthProviderBindingEntity()
                    .setSubjectId(subjectId)
                    .setProviderCode(providerCode)
                    .setExternalAccountId(dto.getExternalAccountId())
                    .setCredentialRef(dto.getCredentialRef())
                    .setStatus(status);
            entity.setId(snowflakeIdGenerator.nextId());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            bindingMapper.insert(entity);
            return entity;
        }
        existing.setExternalAccountId(dto.getExternalAccountId());
        existing.setCredentialRef(dto.getCredentialRef());
        existing.setStatus(status);
        existing.setUpdatedAt(now);
        bindingMapper.updateById(existing);
        return existing;
    }

    /**
     * 同步运行记录列表。
     *
     * @param providerCode 可选数据源
     * @param subjectId    可选主体
     * @param limit        条数上限
     * @return 记录
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthSyncRunEntity> listSyncRuns(String providerCode, Long subjectId, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return syncRunMapper.selectList(Wrappers.<HealthSyncRunEntity>lambdaQuery()
                .eq(StringUtils.hasText(providerCode), HealthSyncRunEntity::getProviderCode, providerCode)
                .eq(subjectId != null, HealthSyncRunEntity::getSubjectId, subjectId)
                .orderByDesc(HealthSyncRunEntity::getStartedAt)
                .last("LIMIT " + size));
    }

    /**
     * 触发数据源同步并入库。
     *
     * @param providerCode 数据源
     * @param dto          请求
     * @return 同步任务
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public HealthSyncRunEntity sync(String providerCode, HealthSyncRequestDto dto) {
        if (!healthProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "健康模块未启用");
        }
        HealthDataProvider provider = providerRegistry.requireEnabled(providerCode);
        Long subjectId = parseId(dto.getSubjectId(), "主体 ID");
        requireSubject(subjectId);

        Instant now = Instant.now();
        HealthSyncRunEntity run = new HealthSyncRunEntity()
                .setProviderCode(provider.code())
                .setSubjectId(subjectId)
                .setStatus("RUNNING")
                .setFetchedCount(0)
                .setIngestedCount(0)
                .setStartedAt(now);
        run.setId(snowflakeIdGenerator.nextId());
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        syncRunMapper.insert(run);

        if (!provider.supportsRemoteFetch()) {
            finishRun(run, "SKIPPED", 0, 0, "该数据源不支持远程拉取", Instant.now());
            return run;
        }

        try {
            String externalAccountId = resolveExternalAccountId(subjectId, provider.code());
            HealthFetchRequest request = new HealthFetchRequest()
                    .setSubjectId(subjectId)
                    .setExternalAccountId(externalAccountId)
                    .setFrom(dto.getFrom())
                    .setTo(dto.getTo())
                    .setMetricCodes(dto.getMetricCodes());
            List<HealthSampleDraft> drafts = provider.fetch(request);
            int fetched = drafts == null ? 0 : drafts.size();
            int ingested = 0;
            if (drafts != null) {
                for (HealthSampleDraft draft : drafts) {
                    if (ingestDraft(provider.code(), draft, subjectId)) {
                        ingested++;
                    }
                }
            }
            touchBindingSync(subjectId, provider.code(), Instant.now());
            finishRun(run, "SUCCESS", fetched, ingested, null, Instant.now());
        } catch (BusinessException ex) {
            finishRun(run, "FAILED", 0, 0, ex.getMessage(), Instant.now());
        } catch (Exception ex) {
            finishRun(run, "FAILED", 0, 0, truncate(ex.getMessage(), 500), Instant.now());
        }
        return syncRunMapper.selectById(run.getId());
    }

    /**
     * 幂等入库草稿。
     *
     * @param providerCode 数据源
     * @param draft        草稿
     * @param subjectId    主体（覆盖 draft.subjectId 若空）
     * @return true 新写入
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public boolean ingestDraft(String providerCode, HealthSampleDraft draft, Long subjectId) {
        if (draft == null || draft.getValueNum() == null || !StringUtils.hasText(draft.getMetricCode())) {
            return false;
        }
        Long sid = draft.getSubjectId() != null ? draft.getSubjectId() : subjectId;
        if (sid == null) {
            return false;
        }
        requireSubject(sid);
        if (StringUtils.hasText(draft.getSourceSampleId())) {
            Long exists = sampleMapper.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery()
                    .eq(HealthSampleEntity::getProviderCode, providerCode)
                    .eq(HealthSampleEntity::getSourceSampleId, draft.getSourceSampleId().trim()));
            if (exists != null && exists > 0) {
                return false;
            }
        }
        Instant now = Instant.now();
        String metric = draft.getMetricCode().trim().toUpperCase(Locale.ROOT);
        String unit = StringUtils.hasText(draft.getUnit()) ? draft.getUnit().trim() : HealthMetricCodes.defaultUnit(metric);
        HealthSampleEntity entity = new HealthSampleEntity()
                .setSubjectId(sid)
                .setMetricCode(metric)
                .setValueNum(draft.getValueNum())
                .setUnit(unit)
                .setMeasuredAt(draft.getMeasuredAt() != null ? draft.getMeasuredAt() : now)
                .setProviderCode(providerCode)
                .setSourceSampleId(StringUtils.hasText(draft.getSourceSampleId()) ? draft.getSourceSampleId().trim() : null)
                .setQuality(normalizeQuality(draft.getQuality()))
                .setMetaJson(toMetaJson(draft));
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        sampleMapper.insert(entity);
        return true;
    }

    private void finishRun(HealthSyncRunEntity run, String status, int fetched, int ingested,
                           String error, Instant finishedAt) {
        run.setStatus(status);
        run.setFetchedCount(fetched);
        run.setIngestedCount(ingested);
        run.setErrorMessage(error);
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        syncRunMapper.updateById(run);
    }

    private String resolveExternalAccountId(Long subjectId, String providerCode) {
        HealthProviderBindingEntity binding = bindingMapper.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .eq(HealthProviderBindingEntity::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return binding == null ? null : binding.getExternalAccountId();
    }

    private void touchBindingSync(Long subjectId, String providerCode, Instant at) {
        HealthProviderBindingEntity binding = bindingMapper.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .last("LIMIT 1"));
        if (binding == null) {
            return;
        }
        binding.setLastSyncAt(at);
        binding.setUpdatedAt(at);
        bindingMapper.updateById(binding);
    }

    private HealthProviderVo toProviderVo(HealthDataProvider provider) {
        return new HealthProviderVo()
                .setCode(provider.code())
                .setDisplayName(provider.displayName())
                .setEnabled(provider.enabled())
                .setSupportsRemoteFetch(provider.supportsRemoteFetch())
                .setSupportedMetrics(provider.supportedMetrics());
    }

    private HealthSubjectEntity requireSubject(Long id) {
        HealthSubjectEntity entity = subjectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "健康主体不存在");
        }
        return entity;
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

    private String toMetaJson(HealthSampleDraft draft) {
        if (draft.getMeta() == null || draft.getMeta().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(draft.getMeta());
        } catch (JsonProcessingException ex) {
            return null;
        }
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

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
