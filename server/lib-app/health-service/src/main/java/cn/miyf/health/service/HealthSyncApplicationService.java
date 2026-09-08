package cn.miyf.health.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.repository.HealthProviderBindingRepository;
import cn.miyf.health.repository.HealthSampleRepository;
import cn.miyf.health.repository.HealthSyncRunRepository;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthDataProviderRegistry;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 健康同步应用服务：远程拉取、幂等入库与同步运行记录。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Service
public class HealthSyncApplicationService {

    private static final Set<String> QUALITIES = Set.of("NORMAL", "ESTIMATED", "SUSPECT");

    private final HealthProperties healthProperties;
    private final HealthDataProviderRegistry providerRegistry;
    private final HealthCrudApplicationService healthCrudApplicationService;
    private final HealthProviderBindingRepository bindingRepository;
    private final HealthSampleRepository sampleRepository;
    private final HealthSyncRunRepository syncRunRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 构造同步服务。
     *
     * @param healthProperties             配置
     * @param providerRegistry             数据源注册表
     * @param healthCrudApplicationService CRUD（requireSubject）
     * @param bindingRepository            绑定仓储
     * @param sampleRepository             采样仓储
     * @param syncRunRepository            同步运行仓储
     * @param snowflakeIdGenerator         雪花 ID
     * @param objectMapper                 JSON
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSyncApplicationService(HealthProperties healthProperties,
                                        HealthDataProviderRegistry providerRegistry,
                                        HealthCrudApplicationService healthCrudApplicationService,
                                        HealthProviderBindingRepository bindingRepository,
                                        HealthSampleRepository sampleRepository,
                                        HealthSyncRunRepository syncRunRepository,
                                        SnowflakeIdGenerator snowflakeIdGenerator,
                                        ObjectMapper objectMapper) {
        this.healthProperties = healthProperties;
        this.providerRegistry = providerRegistry;
        this.healthCrudApplicationService = healthCrudApplicationService;
        this.bindingRepository = bindingRepository;
        this.sampleRepository = sampleRepository;
        this.syncRunRepository = syncRunRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步运行记录列表。
     *
     * @param providerCode 可选数据源
     * @param subjectId    可选主体
     * @param limit        条数上限
     * @return 记录
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSyncRunEntity> listSyncRuns(String providerCode, Long subjectId, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return syncRunRepository.list(providerCode, subjectId, size);
    }

    /**
     * 列出可远程拉取的 ACTIVE 绑定（供定时同步）。
     *
     * @return 绑定列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthProviderBindingEntity> listActiveRemoteBindings() {
        return bindingRepository.listByStatus("ACTIVE").stream()
                .filter(row -> providerRegistry.find(row.getProviderCode())
                        .filter(p -> p.enabled() && p.supportsRemoteFetch())
                        .isPresent())
                .toList();
    }

    /**
     * 触发数据源同步并入库。
     *
     * @param providerCode 数据源
     * @param dto          请求
     * @return 同步任务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSyncRunEntity sync(String providerCode, HealthSyncRequestDto dto) {
        if (!healthProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "健康模块未启用");
        }
        HealthDataProvider provider = providerRegistry.requireEnabled(providerCode);
        Long subjectId = parseId(dto.getSubjectId(), "主体 ID");
        healthCrudApplicationService.requireSubject(subjectId);

        Instant now = Instant.now();
        HealthSyncRunEntity run = new HealthSyncRunEntity()
                .setProviderCode(provider.code())
                .setSubjectId(subjectId)
                .setStatus("RUNNING")
                .setFetchedCount(0)
                .setIngestedCount(0)
                .setStartedTime(now);
        run.setId(snowflakeIdGenerator.nextId());
        run.setCreateTime(now);
        run.setLastModifyTime(now);
        syncRunRepository.insert(run);

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
        return syncRunRepository.findById(run.getId()).orElse(run);
    }

    /**
     * 幂等入库草稿。
     *
     * @param providerCode 数据源
     * @param draft        草稿
     * @param subjectId    主体（覆盖 draft.subjectId 若空）
     * @return true 新写入
     * @history 1.00 2026-09-08 XieMingJie Created.
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
        healthCrudApplicationService.requireSubject(sid);
        if (StringUtils.hasText(draft.getSourceSampleId())
                && sampleRepository.existsByProviderAndSourceSampleId(providerCode, draft.getSourceSampleId().trim())) {
            return false;
        }
        Instant now = Instant.now();
        String metric = draft.getMetricCode().trim().toUpperCase(Locale.ROOT);
        String unit = StringUtils.hasText(draft.getUnit()) ? draft.getUnit().trim() : HealthMetricCodes.defaultUnit(metric);
        HealthSampleEntity entity = new HealthSampleEntity()
                .setSubjectId(sid)
                .setMetricCode(metric)
                .setValueNum(draft.getValueNum())
                .setUnit(unit)
                .setMeasuredTime(draft.getMeasuredTime() != null ? draft.getMeasuredTime() : now)
                .setProviderCode(providerCode)
                .setSourceSampleId(StringUtils.hasText(draft.getSourceSampleId()) ? draft.getSourceSampleId().trim() : null)
                .setQuality(normalizeQuality(draft.getQuality()))
                .setMetaJson(toMetaJson(draft));
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        sampleRepository.insert(entity);
        return true;
    }

    private void finishRun(HealthSyncRunEntity run, String status, int fetched, int ingested,
                           String error, Instant finishedTime) {
        run.setStatus(status);
        run.setFetchedCount(fetched);
        run.setIngestedCount(ingested);
        run.setErrorMessage(error);
        run.setFinishedTime(finishedTime);
        run.setLastModifyTime(finishedTime);
        syncRunRepository.updateById(run);
    }

    private String resolveExternalAccountId(Long subjectId, String providerCode) {
        return bindingRepository.findBySubjectProviderAndStatus(subjectId, providerCode, "ACTIVE")
                .map(HealthProviderBindingEntity::getExternalAccountId)
                .orElse(null);
    }

    private void touchBindingSync(Long subjectId, String providerCode, Instant at) {
        bindingRepository.findBySubjectAndProvider(subjectId, providerCode).ifPresent(binding -> {
            binding.setLastSyncTime(at);
            binding.setLastModifyTime(at);
            bindingRepository.updateById(binding);
        });
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
