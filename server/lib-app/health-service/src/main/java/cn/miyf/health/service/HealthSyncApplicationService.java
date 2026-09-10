package cn.miyf.health.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.health.bean.dto.HealthSyncRequestDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSyncRunEntity;
import cn.miyf.health.bean.vo.HealthSyncRunVo;
import cn.miyf.health.config.HealthProperties;
import cn.miyf.health.domain.HealthMetricCodes;
import cn.miyf.health.repository.HealthProviderBindingRepository;
import cn.miyf.health.repository.HealthSampleRepository;
import cn.miyf.health.repository.HealthSyncRunRepository;
import cn.miyf.health.spi.HealthDataProvider;
import cn.miyf.health.spi.HealthDataProviderRegistry;
import cn.miyf.health.spi.HealthFetchRequest;
import cn.miyf.health.spi.HealthSampleDraft;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 健康同步应用服务：远程拉取、幂等入库与同步运行记录。
 * 同源采样（provider + sourceSampleId）已存在则跳过；并发冲突依赖库唯一索引并吞掉重复键。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Service
@RequiredArgsConstructor
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
     * 同步运行记录列表（管理端按主体数据范围过滤）。
     *
     * @param providerCode 可选数据源
     * @param subjectId    可选主体
     * @param limit        条数上限
     * @return 记录 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSyncRunVo> listSyncRuns(String providerCode, Long subjectId, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        if (subjectId != null) {
            healthCrudApplicationService.requireAccessibleSubject(subjectId);
            return syncRunRepository.selectList(Wrappers.<HealthSyncRunEntity>lambdaQuery()
                            .eq(StringUtils.hasText(providerCode), HealthSyncRunEntity::getProviderCode, providerCode)
                            .eq(HealthSyncRunEntity::getSubjectId, subjectId)
                            .orderByDesc(HealthSyncRunEntity::getStartedTime)
                            .last("LIMIT " + size))
                    .stream()
                    .map(this::toSyncRunVo)
                    .toList();
        }
        List<Long> allowedSubjectIds = healthCrudApplicationService.resolveAccessibleSubjectIdsOrNull();
        if (allowedSubjectIds != null && allowedSubjectIds.isEmpty()) {
            return List.of();
        }
        var query = Wrappers.<HealthSyncRunEntity>lambdaQuery()
                .eq(StringUtils.hasText(providerCode), HealthSyncRunEntity::getProviderCode, providerCode)
                .orderByDesc(HealthSyncRunEntity::getStartedTime)
                .last("LIMIT " + size);
        // null = ALL；非空则 SQL 一次过滤，避免 N+1 鉴权查询
        if (allowedSubjectIds != null) {
            query.in(HealthSyncRunEntity::getSubjectId, allowedSubjectIds);
        }
        return syncRunRepository.selectList(query).stream().map(this::toSyncRunVo).toList();
    }

    /**
     * 列出可远程拉取的 ACTIVE 绑定（供定时同步）。
     *
     * @return 绑定列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthProviderBindingEntity> listActiveRemoteBindings() {
        return bindingRepository.selectList(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                        .eq(HealthProviderBindingEntity::getStatus, "ACTIVE")
                        .orderByAsc(HealthProviderBindingEntity::getProviderCode)
                        .orderByAsc(HealthProviderBindingEntity::getSubjectId))
                .stream()
                .filter(row -> providerRegistry.find(row.getProviderCode())
                        .filter(p -> p.enabled() && p.supportsRemoteFetch())
                        .isPresent())
                .toList();
    }

    /**
     * 触发数据源同步并入库。
     * 管理端调用时校验 DataScope；定时任务无登录上下文时仅校验主体存在。
     *
     * @param providerCode 数据源
     * @param dto          请求
     * @return 同步任务实体（任务内部使用）；对外 API 再转 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSyncRunEntity sync(String providerCode, HealthSyncRequestDto dto) {
        if (!healthProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "健康模块未启用");
        }
        HealthDataProvider provider = providerRegistry.requireEnabled(providerCode);
        Long subjectId = parseId(dto.getSubjectId(), "主体 ID");
        healthCrudApplicationService.requireSubjectForSync(subjectId);

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
        return Optional.ofNullable(syncRunRepository.selectById(run.getId())).orElse(run);
    }

    /**
     * 触发同步并返回脱敏 VO（管理端 API）。
     *
     * @param providerCode 数据源
     * @param dto          请求
     * @return 同步运行 VO
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Transactional
    public HealthSyncRunVo syncAsVo(String providerCode, HealthSyncRequestDto dto) {
        return toSyncRunVo(sync(providerCode, dto));
    }

    /**
     * 个人端同步：强制主体为当前用户自己的健康主体。
     *
     * @param providerCode 数据源
     * @param dto          可选时间窗（subjectId 忽略）
     * @return 同步运行 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public HealthSyncRunVo syncMineAsVo(String providerCode, HealthSyncRequestDto dto) {
        Long subjectId = healthCrudApplicationService.getOrCreateMySubjectEntity().getId();
        HealthSyncRequestDto request = dto == null ? new HealthSyncRequestDto() : dto;
        request.setSubjectId(String.valueOf(subjectId));
        return toSyncRunVo(sync(providerCode, request));
    }

    /**
     * 幂等入库草稿。
     * 草稿若携带 subjectId，必须与本次同步请求主体一致，否则拒绝，防止 Provider 覆盖目标主体。
     *
     * @param providerCode 数据源
     * @param draft        草稿
     * @param subjectId    本次同步目标主体
     * @return true 新写入
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public boolean ingestDraft(String providerCode, HealthSampleDraft draft, Long subjectId) {
        if (draft == null || draft.getValueNum() == null || !StringUtils.hasText(draft.getMetricCode())) {
            return false;
        }
        if (subjectId == null) {
            return false;
        }
        // Provider 不得改写同步目标主体
        if (draft.getSubjectId() != null && !subjectId.equals(draft.getSubjectId())) {
            return false;
        }
        Long sid = subjectId;
        healthCrudApplicationService.requireSubject(sid);
        if (StringUtils.hasText(draft.getSourceSampleId())
                && existsSample(providerCode, draft.getSourceSampleId().trim())) {
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
        try {
            sampleRepository.insert(entity);
            return true;
        } catch (DuplicateKeyException ex) {
            // 并发同步命中 uk_health_sample_provider_source，视为已入库
            return false;
        }
    }

    private HealthSyncRunVo toSyncRunVo(HealthSyncRunEntity entity) {
        return new HealthSyncRunVo()
                .setId(entity.getId())
                .setProviderCode(entity.getProviderCode())
                .setSubjectId(entity.getSubjectId())
                .setStatus(entity.getStatus())
                .setFetchedCount(entity.getFetchedCount())
                .setIngestedCount(entity.getIngestedCount())
                .setErrorMessage(entity.getErrorMessage())
                .setStartedTime(entity.getStartedTime())
                .setFinishedTime(entity.getFinishedTime());
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
        return Optional.ofNullable(bindingRepository.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                        .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                        .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                        .eq(HealthProviderBindingEntity::getStatus, "ACTIVE")
                        .last("LIMIT 1")))
                .map(HealthProviderBindingEntity::getExternalAccountId)
                .orElse(null);
    }

    private void touchBindingSync(Long subjectId, String providerCode, Instant at) {
        Optional.ofNullable(bindingRepository.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                        .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                        .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                        .last("LIMIT 1")))
                .ifPresent(binding -> {
                    binding.setLastSyncTime(at);
                    binding.setLastModifyTime(at);
                    bindingRepository.updateById(binding);
                });
    }

    /**
     * 按数据源与源采样 ID 判断是否已入库（幂等键）。
     *
     * @param providerCode   数据源编码
     * @param sourceSampleId 源侧采样 ID
     * @return true 已存在
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    private boolean existsSample(String providerCode, String sourceSampleId) {
        Long n = sampleRepository.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(HealthSampleEntity::getProviderCode, providerCode)
                .eq(HealthSampleEntity::getSourceSampleId, sourceSampleId));
        return n != null && n > 0;
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
