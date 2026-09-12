package cn.miyf.health.service;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.auth.security.PrincipalType;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.health.bean.dto.HealthMySampleSaveDto;
import cn.miyf.health.bean.dto.HealthProviderBindingSaveDto;
import cn.miyf.health.bean.dto.HealthSampleSaveDto;
import cn.miyf.health.bean.dto.HealthSubjectSaveDto;
import cn.miyf.health.bean.entity.HealthProviderBindingEntity;
import cn.miyf.health.bean.entity.HealthSampleEntity;
import cn.miyf.health.bean.entity.HealthSubjectEntity;
import cn.miyf.health.bean.vo.HealthOverviewVo;
import cn.miyf.health.bean.vo.HealthProviderBindingVo;
import cn.miyf.health.bean.vo.HealthProviderVo;
import cn.miyf.health.bean.vo.HealthSampleVo;
import cn.miyf.health.bean.vo.HealthSubjectVo;
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
import cn.miyf.organization.service.DataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 健康 CRUD 应用服务：主体 / 采样 / 绑定 / 概览与趋势。
 * <ul>
 *   <li>管理端：按管理员 DataScope 裁剪（orgUnitId / createdBy）</li>
 *   <li>个人端：仅 {@code externalUserId = 当前 USER} 的自己的主体</li>
 *   <li>OAuth 浏览器回跳：{@link #upsertBindingAfterOAuthRedirect} 仅校验主体存在</li>
 * </ul>
 * Repository 为纯 Mapper；条件查询通过 MyBatis-Plus Wrapper 在本服务组装。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class HealthCrudApplicationService {

    private static final Set<String> GENDERS = Set.of("UNKNOWN", "MALE", "FEMALE");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");
    private static final Set<String> QUALITIES = Set.of("NORMAL", "ESTIMATED", "SUSPECT");
    private static final int SUBJECT_LIST_MAX = 500;

    private final HealthProperties healthProperties;
    private final HealthDataProviderRegistry providerRegistry;
    private final HealthSubjectRepository subjectRepository;
    private final HealthSampleRepository sampleRepository;
    private final HealthProviderBindingRepository bindingRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final HuaweiHealthOAuthService huaweiHealthOAuthService;
    private final DataScopeService dataScopeService;

    /**
     * 模块概览：当前数据范围内主体数、采样数与可用数据源。
     *
     * @return 概览 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthOverviewVo overview() {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        List<Long> subjectIds = listAccessibleSubjectIds(principal);
        long subjectCount;
        long sampleCount;
        if (isUnrestricted(principal)) {
            Long sc = subjectRepository.selectCount(Wrappers.lambdaQuery());
            Long sm = sampleRepository.selectCount(Wrappers.lambdaQuery());
            subjectCount = sc == null ? 0L : sc;
            sampleCount = sm == null ? 0L : sm;
        } else if (subjectIds.isEmpty()) {
            subjectCount = 0L;
            sampleCount = 0L;
        } else {
            subjectCount = subjectIds.size();
            Long sm = sampleRepository.selectCount(Wrappers.<HealthSampleEntity>lambdaQuery()
                    .in(HealthSampleEntity::getSubjectId, subjectIds));
            sampleCount = sm == null ? 0L : sm;
        }
        return new HealthOverviewVo()
                .setEnabled(healthProperties.isEnabled())
                .setSubjectCount(subjectCount)
                .setSampleCount(sampleCount)
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
     * 主体列表（按 DataScope 过滤，最多 {@value #SUBJECT_LIST_MAX} 条）。
     *
     * @param keyword 关键字
     * @return 主体 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSubjectVo> listSubjects(String keyword) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        LambdaQueryWrapper<HealthSubjectEntity> query = Wrappers.<HealthSubjectEntity>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(HealthSubjectEntity::getDisplayName, keyword)
                        .or()
                        .like(HealthSubjectEntity::getRemark, keyword))
                .orderByDesc(HealthSubjectEntity::getLastModifyTime)
                .last("LIMIT " + SUBJECT_LIST_MAX);
        applySubjectDataScope(query, principal);
        return subjectRepository.selectList(query).stream().map(this::toSubjectVo).toList();
    }

    /**
     * 创建主体（写入当前管理员组织与创建人，供后续数据范围校验）。
     *
     * @param dto 请求
     * @return 主体 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectVo createSubject(HealthSubjectSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        Instant now = Instant.now();
        HealthSubjectEntity entity = mapSubject(new HealthSubjectEntity(), dto);
        entity.setOrgUnitId(resolveOrgUnitIdForWrite(principal, dto.getOrgUnitId(), true));
        entity.setCreatedBy(principal.getId());
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        subjectRepository.insert(entity);
        return toSubjectVo(entity);
    }

    /**
     * 更新主体。
     *
     * @param id  ID
     * @param dto 请求
     * @return 主体 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectVo updateSubject(Long id, HealthSubjectSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        HealthSubjectEntity entity = requireAccessibleSubject(id, principal);
        mapSubject(entity, dto);
        // 仅 ALL 允许改组织；其它范围保持原组织，避免越权迁移
        if (principal.getDataScope() == DataScope.ALL && StringUtils.hasText(dto.getOrgUnitId())) {
            entity.setOrgUnitId(resolveOrgUnitIdForWrite(principal, dto.getOrgUnitId(), false));
        }
        entity.setLastModifyTime(Instant.now());
        subjectRepository.updateById(entity);
        return toSubjectVo(entity);
    }

    /**
     * 删除主体（级联采样由 FK CASCADE；绑定显式删除并清华为 token）。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteSubject(Long id) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        requireAccessibleSubject(id, principal);
        huaweiHealthOAuthService.clearToken(id);
        bindingRepository.delete(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, id));
        subjectRepository.deleteById(id);
    }

    /**
     * 采样列表（按主体 DataScope 过滤）。
     *
     * @param subjectId  主体
     * @param metricCode 指标
     * @param limit      条数上限
     * @return 采样 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthSampleVo> listSamples(Long subjectId, String metricCode, Integer limit) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        int rows = limit == null || limit < 1 ? 100 : Math.min(limit, 500);
        if (subjectId != null) {
            requireAccessibleSubject(subjectId, principal);
        }
        LambdaQueryWrapper<HealthSampleEntity> query = Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(subjectId != null, HealthSampleEntity::getSubjectId, subjectId)
                .eq(StringUtils.hasText(metricCode), HealthSampleEntity::getMetricCode,
                        StringUtils.hasText(metricCode) ? HealthMetricCodes.normalize(metricCode) : metricCode)
                .orderByDesc(HealthSampleEntity::getMeasuredTime)
                .last("LIMIT " + rows);
        if (subjectId == null && !isUnrestricted(principal)) {
            List<Long> allowed = listAccessibleSubjectIds(principal);
            if (allowed.isEmpty()) {
                return List.of();
            }
            query.in(HealthSampleEntity::getSubjectId, allowed);
        }
        return sampleRepository.selectList(query).stream().map(this::toSampleVo).toList();
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
        requireAccessibleSubject(subjectId);
        String metric = HealthMetricCodes.normalize(metricCode);
        if (!StringUtils.hasText(metric)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "metricCode 不能为空");
        }
        int rows = limit == null || limit < 1 ? 200 : Math.min(limit, 1000);
        List<HealthSampleEntity> samples = sampleRepository.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                .eq(HealthSampleEntity::getSubjectId, subjectId)
                .eq(HealthSampleEntity::getMetricCode, metric)
                .ge(from != null, HealthSampleEntity::getMeasuredTime, from)
                .le(to != null, HealthSampleEntity::getMeasuredTime, to)
                .orderByAsc(HealthSampleEntity::getMeasuredTime)
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
     * @return 采样 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthSampleVo createManualSample(HealthSampleSaveDto dto) {
        Long subjectId = parseId(dto.getSubjectId(), "主体 ID");
        requireAccessibleSubject(subjectId);
        String metric = HealthMetricCodes.normalize(dto.getMetricCode());
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
        return toSampleVo(entity);
    }

    /**
     * 删除采样。
     *
     * @param id ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteSample(Long id) {
        HealthSampleEntity entity = Optional.ofNullable(sampleRepository.selectById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "采样不存在"));
        requireAccessibleSubject(entity.getSubjectId());
        sampleRepository.deleteById(entity.getId());
    }

    /**
     * 列出主体的数据源绑定（脱敏 VO）。
     *
     * @param subjectId 主体
     * @return 绑定 VO 列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<HealthProviderBindingVo> listBindings(Long subjectId) {
        requireAccessibleSubject(subjectId);
        return bindingRepository.selectList(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                        .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                        .orderByAsc(HealthProviderBindingEntity::getProviderCode))
                .stream()
                .map(this::toBindingVo)
                .toList();
    }

    /**
     * 创建或更新数据源绑定；响应为脱敏 VO，不回传 credentialRef。
     * 需当前登录可访问该主体。
     *
     * @param subjectId 主体
     * @param dto       请求
     * @return 绑定 VO
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public HealthProviderBindingVo upsertBinding(Long subjectId, HealthProviderBindingSaveDto dto) {
        requireAccessibleSubject(subjectId);
        return doUpsertBinding(subjectId, dto);
    }

    /**
     * OAuth 浏览器回跳落库绑定：仅校验主体存在（无登录会话，依赖已消费的一次性 state）。
     *
     * @param subjectId 主体
     * @param dto       请求
     * @return 绑定 VO
     * @history 1.00 2026-09-11 XieMingJie Created.
     */
    @Transactional
    public HealthProviderBindingVo upsertBindingAfterOAuthRedirect(Long subjectId, HealthProviderBindingSaveDto dto) {
        requireSubject(subjectId);
        return doUpsertBinding(subjectId, dto);
    }

    /**
     * 绑定落库核心逻辑（调用方负责访问控制）。
     *
     * @param subjectId 主体
     * @param dto       请求
     * @return 脱敏绑定 VO
     */
    private HealthProviderBindingVo doUpsertBinding(Long subjectId, HealthProviderBindingSaveDto dto) {
        String providerCode = dto.getProviderCode().trim();
        providerRegistry.find(providerCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "健康数据源不存在: " + providerCode));
        String status = StringUtils.hasText(dto.getStatus())
                ? dto.getStatus().trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if (!Set.of("ACTIVE", "INACTIVE", "REVOKED").contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的绑定状态");
        }
        Instant now = Instant.now();
        HealthProviderBindingEntity saved = findBinding(subjectId, providerCode)
                .map(existing -> {
                    if ("REVOKED".equals(status)) {
                        existing.setCredentialRef(null);
                        existing.setExternalAccountId(null);
                    } else {
                        existing.setExternalAccountId(dto.getExternalAccountId());
                        // 空/缺省不覆盖已有凭证引用，避免表单回写清空
                        if (StringUtils.hasText(dto.getCredentialRef())) {
                            existing.setCredentialRef(dto.getCredentialRef().trim());
                        }
                    }
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
                            .setCredentialRef(StringUtils.hasText(dto.getCredentialRef())
                                    ? dto.getCredentialRef().trim() : null)
                            .setStatus(status);
                    entity.setId(snowflakeIdGenerator.nextId());
                    entity.setCreateTime(now);
                    entity.setLastModifyTime(now);
                    bindingRepository.insert(entity);
                    return entity;
                });
        return toBindingVo(saved);
    }

    /**
     * 仅校验主体存在（供定时同步等无登录上下文场景）。
     *
     * @param id 主体 ID
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthSubjectEntity requireSubject(Long id) {
        return Optional.ofNullable(subjectRepository.selectById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "健康主体不存在"));
    }

    /**
     * 校验主体可被当前登录者访问：管理员走 DataScope，用户仅能访问自己的主体。
     *
     * @param id 主体 ID
     * @return 实体
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public HealthSubjectEntity requireAccessibleSubject(Long id) {
        AuthPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.getType() == PrincipalType.ADMIN) {
            return requireAccessibleSubject(id, principal);
        }
        if (principal.getType() == PrincipalType.USER) {
            return requireOwnSubject(id, principal.getId());
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该健康主体");
    }

    /**
     * 校验主体存在且在指定管理员数据范围内。
     *
     * @param id        主体 ID
     * @param principal 管理员
     * @return 实体
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public HealthSubjectEntity requireAccessibleSubject(Long id, AuthPrincipal principal) {
        HealthSubjectEntity entity = requireSubject(id);
        if (!canAccessSubject(principal, entity)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该健康主体（数据范围限制）");
        }
        return entity;
    }

    /**
     * 管理员走 DataScope；用户校验归属；定时任务无登录上下文时仅校验主体存在。
     *
     * @param id 主体 ID
     * @return 实体
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public HealthSubjectEntity requireSubjectForSync(Long id) {
        Optional<AuthPrincipal> opt = SecurityUtils.currentPrincipal();
        if (opt.isEmpty()) {
            return requireSubject(id);
        }
        AuthPrincipal principal = opt.get();
        if (principal.getType() == PrincipalType.ADMIN) {
            return requireAccessibleSubject(id, principal);
        }
        if (principal.getType() == PrincipalType.USER) {
            return requireOwnSubject(id, principal.getId());
        }
        return requireSubject(id);
    }

    /**
     * 获取或自动创建当前用户的健康主体。
     *
     * @return 主体 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectVo getOrCreateMySubject() {
        return toSubjectVo(getOrCreateMySubjectEntity());
    }

    /**
     * 获取或自动创建当前用户的健康主体实体。
     *
     * @return 实体
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectEntity getOrCreateMySubjectEntity() {
        AuthPrincipal user = SecurityUtils.requireUser();
        HealthSubjectEntity existing = findSubjectByExternalUserId(user.getId());
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        String displayName = StringUtils.hasText(user.getUsername()) ? user.getUsername().trim() : "我";
        HealthSubjectEntity entity = new HealthSubjectEntity()
                .setDisplayName(displayName)
                .setGender("UNKNOWN")
                .setExternalUserId(user.getId())
                .setOrgUnitId(user.getOrgUnitId())
                .setCreatedBy(user.getId())
                .setStatus("ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        subjectRepository.insert(entity);
        return entity;
    }

    /**
     * 更新当前用户自己的健康主体资料（不可改 externalUserId / orgUnitId）。
     *
     * @param dto 请求
     * @return 主体 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public HealthSubjectVo updateMySubject(HealthSubjectSaveDto dto) {
        HealthSubjectEntity entity = getOrCreateMySubjectEntity();
        entity.setDisplayName(dto.getDisplayName().trim())
                .setGender(normalizeGender(dto.getGender()))
                .setBirthDate(dto.getBirthDate())
                .setHeightCm(dto.getHeightCm())
                .setStatus(normalizeStatus(dto.getStatus()))
                .setRemark(dto.getRemark())
                .setLastModifyTime(Instant.now());
        subjectRepository.updateById(entity);
        return toSubjectVo(entity);
    }

    /**
     * 当前用户的采样列表。
     *
     * @param metricCode 指标
     * @param limit      条数
     * @return 采样 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public List<HealthSampleVo> listMySamples(String metricCode, Integer limit) {
        Long subjectId = getOrCreateMySubjectEntity().getId();
        int rows = limit == null || limit < 1 ? 100 : Math.min(limit, 500);
        return sampleRepository.selectList(Wrappers.<HealthSampleEntity>lambdaQuery()
                        .eq(HealthSampleEntity::getSubjectId, subjectId)
                        .eq(StringUtils.hasText(metricCode), HealthSampleEntity::getMetricCode,
                                StringUtils.hasText(metricCode) ? HealthMetricCodes.normalize(metricCode) : metricCode)
                        .orderByDesc(HealthSampleEntity::getMeasuredTime)
                        .last("LIMIT " + rows))
                .stream()
                .map(this::toSampleVo)
                .toList();
    }

    /**
     * 当前用户手动录入采样。
     *
     * @param dto 请求
     * @return 采样 VO
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public HealthSampleVo createMyManualSample(HealthMySampleSaveDto dto) {
        Long subjectId = getOrCreateMySubjectEntity().getId();
        HealthSampleSaveDto adminDto = new HealthSampleSaveDto()
                .setSubjectId(String.valueOf(subjectId))
                .setMetricCode(dto.getMetricCode())
                .setValueNum(dto.getValueNum())
                .setUnit(dto.getUnit())
                .setMeasuredTime(dto.getMeasuredTime())
                .setQuality(dto.getQuality())
                .setMetaJson(dto.getMetaJson());
        return createManualSample(adminDto);
    }

    /**
     * 删除当前用户自己的采样。
     *
     * @param id 采样 ID
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    @Transactional
    public void deleteMySample(Long id) {
        HealthSampleEntity entity = Optional.ofNullable(sampleRepository.selectById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "采样不存在"));
        requireOwnSubject(entity.getSubjectId(), SecurityUtils.currentUserId());
        sampleRepository.deleteById(entity.getId());
    }

    /**
     * 当前用户指标趋势。
     *
     * @param metricCode 指标
     * @param from       起始
     * @param to         结束
     * @param limit      点数
     * @return 趋势
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public HealthTrendVo myTrend(String metricCode, Instant from, Instant to, Integer limit) {
        Long subjectId = getOrCreateMySubjectEntity().getId();
        return trend(subjectId, metricCode, from, to, limit);
    }

    /**
     * 当前用户的数据源绑定。
     *
     * @return 绑定列表
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public List<HealthProviderBindingVo> listMyBindings() {
        return listBindings(getOrCreateMySubjectEntity().getId());
    }

    private HealthSubjectEntity requireOwnSubject(Long id, Long userId) {
        HealthSubjectEntity entity = requireSubject(id);
        if (!Objects.equals(userId, entity.getExternalUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该健康主体");
        }
        return entity;
    }

    private HealthSubjectEntity findSubjectByExternalUserId(Long externalUserId) {
        if (externalUserId == null) {
            return null;
        }
        return subjectRepository.selectOne(Wrappers.<HealthSubjectEntity>lambdaQuery()
                .eq(HealthSubjectEntity::getExternalUserId, externalUserId)
                .orderByAsc(HealthSubjectEntity::getId)
                .last("LIMIT 1"));
    }

    private boolean canAccessSubject(AuthPrincipal principal, HealthSubjectEntity subject) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return true;
        }
        if (scope == DataScope.SELF) {
            return Objects.equals(principal.getId(), subject.getCreatedBy());
        }
        Set<Long> allowed = dataScopeService.resolveAllowedOrgIds(principal);
        return subject.getOrgUnitId() != null && allowed != null && allowed.contains(subject.getOrgUnitId());
    }

    private boolean isUnrestricted(AuthPrincipal principal) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        return scope == DataScope.ALL;
    }

    private void applySubjectDataScope(LambdaQueryWrapper<HealthSubjectEntity> query, AuthPrincipal principal) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return;
        }
        if (scope == DataScope.SELF) {
            query.eq(HealthSubjectEntity::getCreatedBy, principal.getId());
            return;
        }
        Set<Long> allowed = dataScopeService.resolveAllowedOrgIds(principal);
        if (allowed == null || allowed.isEmpty()) {
            // 无可见组织时强制无结果
            query.eq(HealthSubjectEntity::getId, -1L);
            return;
        }
        query.in(HealthSubjectEntity::getOrgUnitId, allowed);
    }

    /**
     * 解析当前管理员可访问的主体 ID。
     * <ul>
     *   <li>返回 {@code null}：DataScope.ALL，不限制</li>
     *   <li>返回空列表：无可访问主体</li>
     *   <li>返回非空：仅这些主体 ID 可见</li>
     * </ul>
     *
     * @return 主体 ID 集合或 null
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    public List<Long> resolveAccessibleSubjectIdsOrNull() {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        if (isUnrestricted(principal)) {
            return null;
        }
        return listAccessibleSubjectIds(principal);
    }

    private List<Long> listAccessibleSubjectIds(AuthPrincipal principal) {
        if (isUnrestricted(principal)) {
            return subjectRepository.selectList(Wrappers.<HealthSubjectEntity>lambdaQuery()
                            .select(HealthSubjectEntity::getId))
                    .stream()
                    .map(HealthSubjectEntity::getId)
                    .filter(Objects::nonNull)
                    .toList();
        }
        LambdaQueryWrapper<HealthSubjectEntity> query = Wrappers.<HealthSubjectEntity>lambdaQuery()
                .select(HealthSubjectEntity::getId);
        applySubjectDataScope(query, principal);
        return subjectRepository.selectList(query).stream()
                .map(HealthSubjectEntity::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long resolveOrgUnitIdForWrite(AuthPrincipal principal, String rawOrgUnitId, boolean creating) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return parseOptionalId(rawOrgUnitId);
        }
        if (scope == DataScope.SELF) {
            return principal.getOrgUnitId();
        }
        Long orgId = parseOptionalId(rawOrgUnitId);
        if (orgId == null) {
            orgId = principal.getOrgUnitId();
        }
        if (creating || orgId != null) {
            dataScopeService.assertCanAssignOrg(principal, orgId);
        }
        return orgId;
    }

    private Optional<HealthProviderBindingEntity> findBinding(Long subjectId, String providerCode) {
        return Optional.ofNullable(bindingRepository.selectOne(Wrappers.<HealthProviderBindingEntity>lambdaQuery()
                .eq(HealthProviderBindingEntity::getSubjectId, subjectId)
                .eq(HealthProviderBindingEntity::getProviderCode, providerCode)
                .last("LIMIT 1")));
    }

    private HealthProviderVo toProviderVo(HealthDataProvider provider) {
        return new HealthProviderVo()
                .setCode(provider.code())
                .setDisplayName(provider.displayName())
                .setEnabled(provider.enabled())
                .setSupportsRemoteFetch(provider.supportsRemoteFetch())
                .setSupportedMetrics(provider.supportedMetrics());
    }

    private HealthSubjectVo toSubjectVo(HealthSubjectEntity entity) {
        return new HealthSubjectVo()
                .setId(entity.getId())
                .setDisplayName(entity.getDisplayName())
                .setGender(entity.getGender())
                .setBirthDate(entity.getBirthDate())
                .setHeightCm(entity.getHeightCm())
                .setExternalUserId(entity.getExternalUserId())
                .setOrgUnitId(entity.getOrgUnitId())
                .setCreatedBy(entity.getCreatedBy())
                .setStatus(entity.getStatus())
                .setRemark(entity.getRemark())
                .setCreateTime(entity.getCreateTime())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    private HealthSampleVo toSampleVo(HealthSampleEntity entity) {
        return new HealthSampleVo()
                .setId(entity.getId())
                .setSubjectId(entity.getSubjectId())
                .setMetricCode(entity.getMetricCode())
                .setValueNum(entity.getValueNum())
                .setUnit(entity.getUnit())
                .setMeasuredTime(entity.getMeasuredTime())
                .setProviderCode(entity.getProviderCode())
                .setQuality(entity.getQuality())
                .setCreateTime(entity.getCreateTime());
    }

    private HealthProviderBindingVo toBindingVo(HealthProviderBindingEntity entity) {
        String displayName = providerRegistry.find(entity.getProviderCode())
                .map(HealthDataProvider::displayName)
                .orElse(entity.getProviderCode());
        return new HealthProviderBindingVo()
                .setId(entity.getId())
                .setSubjectId(entity.getSubjectId())
                .setProviderCode(entity.getProviderCode())
                .setProviderDisplayName(displayName)
                .setExternalAccountMasked(maskAccount(entity.getExternalAccountId()))
                .setHasCredential(StringUtils.hasText(entity.getCredentialRef()))
                .setStatus(entity.getStatus())
                .setLastSyncTime(entity.getLastSyncTime());
    }

    private String maskAccount(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String v = raw.trim();
        if (v.length() <= 4) {
            return "****";
        }
        return "****" + v.substring(v.length() - 4);
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ID 非法");
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
