package cn.miyf.service;

import cn.miyf.bean.dto.SysConfigSaveDto;
import cn.miyf.bean.entity.SysConfigEntity;
import cn.miyf.bean.vo.SysConfigVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysConfigMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 系统配置应用服务。
 * 管理端读写经 VO 脱敏；敏感配置更新时空值保留原值。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
@RequiredArgsConstructor
public class SystemConfigApplicationService {

    private static final Set<String> VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");

    private final SysConfigMapper configMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SystemConfigReader systemConfigReader;

    /**
     * 配置列表（脱敏）。
     *
     * @param groupCode 可选分组
     * @param keyword   可选关键字（键/名称）
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<SysConfigVo> list(String groupCode, String keyword) {
        return configMapper.selectList(Wrappers.<SysConfigEntity>lambdaQuery()
                        .eq(StringUtils.hasText(groupCode), SysConfigEntity::getGroupCode, groupCode)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysConfigEntity::getConfigKey, keyword)
                                .or()
                                .like(SysConfigEntity::getName, keyword))
                        .orderByAsc(SysConfigEntity::getGroupCode)
                        .orderByAsc(SysConfigEntity::getSortOrder)
                        .orderByAsc(SysConfigEntity::getConfigKey))
                .stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 按键查询（脱敏）。
     *
     * @param configKey 配置键
     * @return VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public SysConfigVo getByKey(String configKey) {
        return toVo(requireByKey(configKey));
    }

    /**
     * 创建配置。
     *
     * @param dto 请求
     * @return VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysConfigVo create(SysConfigSaveDto dto) {
        assertKeyUnique(dto.getConfigKey(), null);
        Instant now = Instant.now();
        SysConfigEntity entity = mapDto(new SysConfigEntity(), dto, true);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        configMapper.insert(entity);
        systemConfigReader.invalidate(entity.getConfigKey());
        return toVo(entity);
    }

    /**
     * 更新配置；敏感项 configValue 为空时保留原值。
     *
     * @param id  ID
     * @param dto 请求
     * @return VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysConfigVo update(Long id, SysConfigSaveDto dto) {
        SysConfigEntity entity = require(id);
        assertKeyUnique(dto.getConfigKey(), id);
        mapDto(entity, dto, false);
        entity.setLastModifyTime(Instant.now());
        configMapper.updateById(entity);
        systemConfigReader.invalidateAll();
        return toVo(entity);
    }

    /**
     * 删除配置。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        SysConfigEntity entity = require(id);
        configMapper.deleteById(id);
        systemConfigReader.invalidate(entity.getConfigKey());
    }

    private SysConfigEntity require(Long id) {
        SysConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在");
        }
        return entity;
    }

    private SysConfigEntity requireByKey(String configKey) {
        SysConfigEntity entity = configMapper.selectOne(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(SysConfigEntity::getConfigKey, configKey)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在");
        }
        return entity;
    }

    private void assertKeyUnique(String configKey, Long excludeId) {
        Long cnt = configMapper.selectCount(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(SysConfigEntity::getConfigKey, configKey)
                .ne(excludeId != null, SysConfigEntity::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "配置键已存在");
        }
    }

    private SysConfigEntity mapDto(SysConfigEntity entity, SysConfigSaveDto dto, boolean creating) {
        String valueType = normalizeValueType(dto.getValueType());
        String status = normalizeStatus(dto.getStatus());
        String group = StringUtils.hasText(dto.getGroupCode()) ? dto.getGroupCode().trim() : "default";
        boolean sensitive = dto.getSensitive() != null
                ? dto.getSensitive()
                : (creating ? inferSensitive(dto.getConfigKey(), group) : Boolean.TRUE.equals(entity.getSensitive()));
        entity.setConfigKey(dto.getConfigKey().trim())
                .setValueType(valueType)
                .setGroupCode(group)
                .setName(dto.getName().trim())
                .setDescription(dto.getDescription())
                .setStatus(status)
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setSensitive(sensitive);
        // 敏感配置更新时空值保留原值；非敏感或新建按 DTO 写入
        if (creating || !sensitive || StringUtils.hasText(dto.getConfigValue())) {
            entity.setConfigValue(dto.getConfigValue());
        }
        return entity;
    }

    private boolean inferSensitive(String configKey, String groupCode) {
        String key = configKey == null ? "" : configKey.toLowerCase(Locale.ROOT);
        String group = groupCode == null ? "" : groupCode.toLowerCase(Locale.ROOT);
        return group.contains("security")
                || key.contains("secret")
                || key.contains("password")
                || key.contains("token")
                || key.contains("credential")
                || key.contains("webhook");
    }

    private SysConfigVo toVo(SysConfigEntity entity) {
        boolean sensitive = Boolean.TRUE.equals(entity.getSensitive());
        boolean configured = StringUtils.hasText(entity.getConfigValue());
        return new SysConfigVo()
                .setId(entity.getId())
                .setConfigKey(entity.getConfigKey())
                .setConfigValue(sensitive ? null : entity.getConfigValue())
                .setSensitive(sensitive)
                .setConfigured(configured)
                .setValueType(entity.getValueType())
                .setGroupCode(entity.getGroupCode())
                .setName(entity.getName())
                .setDescription(entity.getDescription())
                .setStatus(entity.getStatus())
                .setSortOrder(entity.getSortOrder())
                .setLastModifyTime(entity.getLastModifyTime());
    }

    private String normalizeValueType(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "STRING";
        if (!VALUE_TYPES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的值类型");
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
}
